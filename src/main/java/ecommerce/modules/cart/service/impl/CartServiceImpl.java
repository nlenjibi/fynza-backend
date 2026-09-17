package ecommerce.modules.cart.service.impl;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.cart.dto.*;
import ecommerce.modules.cart.entity.*;
import ecommerce.modules.cart.event.*;
import ecommerce.modules.cart.exception.CartErrorCode;
import ecommerce.modules.cart.exception.CartException;
import ecommerce.modules.cart.repository.CartItemRepository;
import ecommerce.modules.cart.repository.CartRepository;
import ecommerce.modules.cart.repository.StockReservationRepository;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.coupon.entity.Coupon;
import ecommerce.modules.coupon.repository.CouponRepository;
import ecommerce.modules.inventory.service.InventoryService;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.service.PriceResolverService;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final int  MAX_ITEM_QUANTITY   = 9_999;
    private static final int  GUEST_CART_TTL_DAYS = 7;
    private static final SecureRandom RANDOM      = new SecureRandom();

    private final CartRepository              cartRepository;
    private final CartItemRepository          cartItemRepository;
    private final StockReservationRepository  reservationRepository;
    private final CouponRepository            couponRepository;
    private final PriceResolverService        priceResolverService;
    private final ProductRepository           productRepository;
    private final ProductVariantRepository    variantRepository;
    private final InventoryService            inventoryService;
    private final CartEventPublisher          eventPublisher;

    // ── Guest cart ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GuestCartResponse createGuestCart() {
        String token = generateCartToken();
        Cart cart = Cart.builder()
                .isGuest(true)
                .cartToken(token)
                .status(CartStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(GUEST_CART_TTL_DAYS * 86_400L))
                .build();
        cart = cartRepository.save(cart);
        eventPublisher.publish(new GuestCartCreatedEvent(cart.getPublicId(), token));
        log.info("Guest cart created: token={}", token);
        return GuestCartResponse.builder().cartId(cart.getPublicId()).cartToken(token).build();
    }

    @Override
    public CartResponse getGuestCart(String cartToken) {
        Cart cart = findGuestCart(cartToken);
        return toResponse(cart);
    }

    // ── Authenticated cart ────────────────────────────────────────────────────

    @Override
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateUserCart(userId);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartItemResponse addItem(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateUserCart(userId);
        CartItem item = upsertItem(cart, request.getProductId(), request.getVariantId(), request.getQuantity());
        recalculateTotals(cart);
        cartRepository.save(cart);
        eventPublisher.publish(new CartItemAddedEvent(
                cart.getPublicId(), userId,
                request.getProductId(), request.getVariantId(),
                request.getQuantity(), item.getUnitPrice()));
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public CartItemResponse addItemToGuestCart(String cartToken, AddToCartRequest request) {
        Cart cart = findGuestCart(cartToken);
        CartItem item = upsertItem(cart, request.getProductId(), request.getVariantId(), request.getQuantity());
        recalculateTotals(cart);
        cartRepository.save(cart);
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public CartItemResponse updateItemQuantity(UUID userId, UUID cartItemPublicId, int quantity) {
        Cart cart = getOrCreateUserCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndPublicId(cart.getId(), cartItemPublicId)
                .orElseThrow(() -> new CartException(CartErrorCode.ITEM_NOT_FOUND,
                        "Cart item not found: " + cartItemPublicId));
        int oldQty = item.getQuantity();
        item.setQuantity(Math.min(quantity, MAX_ITEM_QUANTITY));
        item.recalculateLineTotal();
        item = cartItemRepository.save(item);
        recalculateTotals(cart);
        cartRepository.save(cart);
        eventPublisher.publish(new CartItemUpdatedEvent(
                cart.getPublicId(), userId, item.getProductId(), item.getVariantId(), oldQty, item.getQuantity()));
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID cartItemPublicId) {
        Cart cart = getOrCreateUserCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndPublicId(cart.getId(), cartItemPublicId)
                .orElseThrow(() -> new CartException(CartErrorCode.ITEM_NOT_FOUND,
                        "Cart item not found: " + cartItemPublicId));
        reservationRepository.findByCartItemId(item.getId()).ifPresent(reservationRepository::delete);
        cartItemRepository.delete(item);
        recalculateTotals(cart);
        cartRepository.save(cart);
        eventPublisher.publish(new CartItemRemovedEvent(
                cart.getPublicId(), userId, item.getProductId(), item.getVariantId(), item.getQuantity()));
    }

    @Override
    @Transactional
    public CartResponse applyCoupon(UUID userId, String couponCode) {
        Cart cart = getOrCreateUserCart(userId);
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new CartException(CartErrorCode.COUPON_NOT_FOUND,
                        "Coupon not found: " + couponCode));
        validateCoupon(coupon, cart);
        cart.setCouponCode(couponCode);
        recalculateTotals(cart);
        cartRepository.save(cart);
        eventPublisher.publish(new CouponAppliedEvent(
                cart.getPublicId(), userId, couponCode, cart.getDiscountAmount()));
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCoupon(UUID userId) {
        Cart cart = getOrCreateUserCart(userId);
        String removed = cart.getCouponCode();
        cart.setCouponCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);
        recalculateTotals(cart);
        cartRepository.save(cart);
        if (removed != null) {
            eventPublisher.publish(new CouponRemovedEvent(cart.getPublicId(), userId, removed));
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateUserCart(userId);
        int count = cart.getItems().size();
        reservationRepository.deleteByCartId(cart.getId());
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setCouponCode(null);
        recalculateTotals(cart);
        cartRepository.save(cart);
        eventPublisher.publish(new CartClearedEvent(cart.getPublicId(), userId, count));
    }

    @Override
    @Transactional
    public CartResponse mergeCart(UUID userId, String guestCartToken) {
        Cart userCart  = getOrCreateUserCart(userId);
        Cart guestCart = findGuestCart(guestCartToken);

        List<CartItem> guestItems = cartItemRepository.findByCartId(guestCart.getId());
        int mergedCount = 0;

        for (CartItem guestItem : guestItems) {
            upsertItem(userCart, guestItem.getProductId(), guestItem.getVariantId(), guestItem.getQuantity());
            mergedCount++;
        }

        guestCart.setStatus(CartStatus.MERGED);
        guestCart.setMergedIntoCartId(userCart.getId());
        cartRepository.save(guestCart);

        recalculateTotals(userCart);
        cartRepository.save(userCart);

        eventPublisher.publish(new CartMergedEvent(
                userCart.getPublicId(), userId, guestCartToken, mergedCount));
        log.info("Merged {} items from guest cart {} into user cart for user={}", mergedCount, guestCartToken, userId);
        return toResponse(userCart);
    }

    @Override
    @Transactional
    public CartResponse refreshPrices(UUID userId) {
        Cart cart = getOrCreateUserCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        for (CartItem item : items) {
            try {
                BigDecimal newPrice = priceResolverService.resolve(
                        item.getProductId(), item.getVariantId(), item.getQuantity(), SupportedCurrency.GHS
                ).getEffectivePrice();
                boolean changed = newPrice.compareTo(item.getUnitPrice()) != 0;
                item.setUnitPrice(newPrice);
                item.setPriceChanged(changed);
                item.setPriceSnapshotAt(Instant.now());
                item.recalculateLineTotal();
                cartItemRepository.save(item);
            } catch (Exception e) {
                log.warn("Price refresh failed for product={}: {}", item.getProductId(), e.getMessage());
                item.setPriceChanged(true);
                cartItemRepository.save(item);
            }
        }
        recalculateTotals(cart);
        cartRepository.save(cart);
        return toResponse(cart);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Cart getOrCreateUserCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId).orElseGet(() -> {
            Cart c = Cart.builder()
                    .userId(userId)
                    .isGuest(false)
                    .status(CartStatus.ACTIVE)
                    .build();
            return cartRepository.save(c);
        });
    }

    private Cart findGuestCart(String cartToken) {
        Cart cart = cartRepository.findByCartToken(cartToken)
                .orElseThrow(() -> new CartException(CartErrorCode.GUEST_CART_NOT_FOUND,
                        "Guest cart not found for token: " + cartToken));
        if (cart.isExpired()) {
            cart.setStatus(CartStatus.EXPIRED);
            cartRepository.save(cart);
            throw new CartException(CartErrorCode.CART_EXPIRED, "Guest cart has expired");
        }
        return cart;
    }

    private CartItem upsertItem(Cart cart, UUID productId, UUID variantId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CartException(CartErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found: " + productId));

        if (!Boolean.TRUE.equals(product.getIsActive()) || product.getStatus() != ProductStatus.ACTIVE) {
            throw new CartException(CartErrorCode.ITEM_UNAVAILABLE,
                    "Product is not available for purchase: " + productId);
        }

        Long storeId = product.getStoreId();

        if (variantId != null) {
            variantRepository.findByIdAndProductId(variantId, productId)
                    .filter(v -> "ACTIVE".equals(v.getVariantStatus()) && Boolean.TRUE.equals(v.getIsActive()))
                    .orElseThrow(() -> new CartException(CartErrorCode.VARIANT_NOT_FOUND,
                            "Variant not found or inactive: " + variantId));
        }

        checkAvailability(productId, variantId, quantity);

        BigDecimal price = resolvePrice(productId, variantId, quantity);

        CartItem existing = cartItemRepository
                .findByCartIdAndProductAndVariant(cart.getId(), productId, variantId)
                .orElse(null);

        if (existing != null) {
            int newQty = (int) Math.min((long) existing.getQuantity() + quantity, MAX_ITEM_QUANTITY);
            checkAvailability(productId, variantId, newQty);
            existing.setQuantity(newQty);
            existing.setUnitPrice(price);
            existing.setPriceSnapshotAt(Instant.now());
            existing.recalculateLineTotal();
            return cartItemRepository.save(existing);
        }

        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(productId)
                .variantId(variantId)
                .storeId(storeId)
                .quantity(quantity)
                .unitPrice(price)
                .lineTotal(price.multiply(BigDecimal.valueOf(quantity)))
                .priceSnapshotAt(Instant.now())
                .priceChanged(false)
                .build();
        return cartItemRepository.save(item);
    }

    private void checkAvailability(UUID productId, UUID variantId, int quantity) {
        try {
            var availability = inventoryService.getAvailability(productId, variantId);
            if (!availability.isAllowBackorder() && availability.getAvailableQuantity() < quantity) {
                throw new CartException(CartErrorCode.ITEM_UNAVAILABLE,
                        "Only " + availability.getAvailableQuantity() + " units available");
            }
        } catch (CartException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Inventory check skipped for product={}: {}", productId, e.getMessage());
        }
    }

    private BigDecimal resolvePrice(UUID productId, UUID variantId, int quantity) {
        try {
            return priceResolverService.resolve(productId, variantId, quantity, SupportedCurrency.GHS)
                    .getEffectivePrice();
        } catch (Exception e) {
            log.warn("Price resolution failed for product={}: {}", productId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void recalculateTotals(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        BigDecimal subtotal = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getCouponCode() != null) {
            discount = computeDiscount(cart.getCouponCode(), subtotal);
        }

        BigDecimal taxable  = subtotal.subtract(discount);
        BigDecimal tax      = taxable.multiply(BigDecimal.valueOf(0.10)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(50)) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(5.99);
        BigDecimal grand    = taxable.add(tax).add(shipping);

        cart.setSubtotal(subtotal.setScale(4, RoundingMode.HALF_UP));
        cart.setDiscountAmount(discount.setScale(4, RoundingMode.HALF_UP));
        cart.setTaxTotal(tax);
        cart.setShippingTotal(shipping.setScale(4, RoundingMode.HALF_UP));
        cart.setGrandTotal(grand.setScale(4, RoundingMode.HALF_UP));
    }

    private BigDecimal computeDiscount(String couponCode, BigDecimal subtotal) {
        return couponRepository.findByCode(couponCode).map(coupon -> {
            if (coupon.getDiscountType() == ecommerce.common.enums.DiscountType.PERCENTAGE) {
                return subtotal.multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }
            return coupon.getDiscountValue();
        }).orElse(BigDecimal.ZERO);
    }

    private void validateCoupon(Coupon coupon, Cart cart) {
        if (coupon.getStatus() != ecommerce.common.enums.CouponStatus.ACTIVE) {
            throw new CartException(CartErrorCode.COUPON_INACTIVE, "Coupon is not active");
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new CartException(CartErrorCode.COUPON_EXPIRED, "Coupon is expired or not yet valid");
        }
        if (coupon.getMaxUses() != null && coupon.getUsageCount() >= coupon.getMaxUses()) {
            throw new CartException(CartErrorCode.COUPON_USAGE_LIMIT_REACHED, "Coupon usage limit reached");
        }
        if (coupon.getMinOrderAmount() != null
                && cart.getSubtotal().compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CartException(CartErrorCode.COUPON_MIN_AMOUNT_NOT_MET,
                    "Minimum order amount not met for this coupon");
        }
    }

    private static String generateCartToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        boolean hasPriceChanges = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getPriceChanged()));
        List<CartItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();
        return CartResponse.builder()
                .id(cart.getPublicId())
                .userId(cart.getUserId())
                .cartToken(cart.getCartToken())
                .status(cart.getStatus())
                .isGuest(cart.getIsGuest())
                .items(itemResponses)
                .couponCode(cart.getCouponCode())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .shippingTotal(cart.getShippingTotal())
                .taxTotal(cart.getTaxTotal())
                .grandTotal(cart.getGrandTotal())
                .itemsCount(itemResponses.size())
                .expiresAt(cart.getExpiresAt())
                .updatedAt(cart.getUpdatedAt())
                .hasPriceChanges(hasPriceChanges)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getPublicId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .storeId(item.getStoreId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .priceChanged(item.getPriceChanged())
                .priceSnapshotAt(item.getPriceSnapshotAt())
                .build();
    }
}
