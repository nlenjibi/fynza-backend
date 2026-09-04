package ecommerce.modules.cart.async;

import ecommerce.common.config.AsyncProperties;
import ecommerce.modules.cart.dto.ReservationResponse;
import ecommerce.modules.cart.entity.CartItem;
import ecommerce.modules.cart.entity.ReservationStatus;
import ecommerce.modules.cart.entity.StockReservation;
import ecommerce.modules.cart.repository.CartItemRepository;
import ecommerce.modules.cart.repository.StockReservationRepository;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static ecommerce.modules.cart.entity.ReservationStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationAsyncService {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final StockReservationRepository reservationRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AsyncProperties asyncProperties;

    @Async("inventoryExecutor")
    @Transactional
    public CompletableFuture<ReservationResponse> reserveStockAsync(UUID cartItemId) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Starting async stock reservation for cart item: {}", correlationId, cartItemId);

        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

            Product product = cartItem.getProduct();
            int requestedQty = cartItem.getQuantity();

            StockReservation reservation = reservationRepository.findByCartItemId(cartItemId)
                    .orElseGet(() -> createPendingReservation(cartItem, product, requestedQty));

            if (reservation.getStatus() == CONFIRMED) {
                log.info("[{}] Stock already reserved for cart item: {}", correlationId, cartItemId);
                return CompletableFuture.completedFuture(ReservationResponse.confirmed(reservation.getId()));
            }

            boolean reserved = attemptReservation(product, requestedQty);

            if (reserved) {
                productRepository.save(product);
                reservation.setStatus(CONFIRMED);
                reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));
                reservationRepository.save(reservation);
                log.info("[{}] Stock reservation confirmed for cart item: {}, qty: {}", 
                        correlationId, cartItemId, requestedQty);
                return CompletableFuture.completedFuture(ReservationResponse.confirmed(reservation.getId()));
            } else {
                return handleReservationFailure(reservation, "Insufficient stock", correlationId);
            }

        } catch (Exception e) {
            log.error("[{}] Error during stock reservation for cart item: {}", correlationId, cartItemId, e);
            return CompletableFuture.completedFuture(
                    ReservationResponse.failed(null, "Reservation failed: " + e.getMessage(), 0));
        }
    }

    @Transactional
    public StockReservation createPendingReservation(CartItem cartItem, Product product, int quantity) {
        StockReservation reservation = StockReservation.builder()
                .cartItem(cartItem)
                .product(product)
                .quantity(quantity)
                .status(PENDING)
                .retryCount(0)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        return reservationRepository.save(reservation);
    }

    private boolean attemptReservation(Product product, int quantity) {
        synchronized (product) {
            if (product.getAvailableQuantity() >= quantity) {
                product.reserveStock(quantity);
                return true;
            }
            return false;
        }
    }

    private CompletableFuture<ReservationResponse> handleReservationFailure(
            StockReservation reservation, String errorMessage, String correlationId) {
        
        reservation.setRetryCount(reservation.getRetryCount() + 1);
        reservation.setErrorMessage(errorMessage);

        if (reservation.getRetryCount() < MAX_RETRIES) {
            long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, reservation.getRetryCount() - 1);
            log.info("[{}] Scheduling retry {} for reservation {} after {}ms",
                    correlationId, reservation.getRetryCount(), reservation.getId(), backoffMs);

            reservationRepository.save(reservation);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return reserveStockAsync(reservation.getCartItem().getId()).join();
            });
        } else {
            reservation.setStatus(FAILED);
            reservation.setErrorMessage("Max retries exceeded: " + errorMessage);
            reservationRepository.save(reservation);
            log.warn("[{}] Stock reservation failed after {} retries for reservation: {}",
                    correlationId, MAX_RETRIES, reservation.getId());
            return CompletableFuture.completedFuture(
                    ReservationResponse.failed(reservation.getId(), 
                            "Reservation failed after " + MAX_RETRIES + " retries", 
                            reservation.getRetryCount()));
        }
    }

    public ReservationResponse getReservationStatus(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .map(res -> ReservationResponse.builder()
                        .reservationId(res.getId())
                        .status(res.getStatus())
                        .retryCount(res.getRetryCount())
                        .message(res.getErrorMessage() != null ? res.getErrorMessage() : 
                                "Reservation " + res.getStatus().name().toLowerCase())
                        .build())
                .orElse(ReservationResponse.failed(null, "Reservation not found", 0));
    }

    public ReservationResponse getReservationByCartItemId(UUID cartItemId) {
        return reservationRepository.findByCartItemId(cartItemId)
                .map(res -> ReservationResponse.builder()
                        .reservationId(res.getId())
                        .status(res.getStatus())
                        .retryCount(res.getRetryCount())
                        .message(res.getErrorMessage() != null ? res.getErrorMessage() : 
                                "Reservation " + res.getStatus().name().toLowerCase())
                        .build())
                .orElse(ReservationResponse.failed(null, "No reservation found for cart item", 0));
    }
}
