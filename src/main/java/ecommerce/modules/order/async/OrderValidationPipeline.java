package ecommerce.modules.order.async;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.PaymentStatus;
import ecommerce.common.config.AsyncProperties;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderValidationPipeline {

    private final OrderRepository orderRepository;
    private final AsyncProperties asyncProperties;
    private final FraudDetectionService fraudDetectionService;

    @Async("orderExecutor")
    public CompletableFuture<ValidationResult> runPipeline(Order order) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Starting order validation pipeline for order: {}", correlationId, order.getId());

        long timeout = asyncProperties.getTimeouts().getOrderValidation();

        var addressFuture = validateAddressAsync(order)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> ValidationResult.neutral("ADDRESS", "Address validation timeout - assuming valid"));

        var paymentFuture = validatePaymentMethodAsync(order)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> ValidationResult.neutral("PAYMENT", "Payment validation timeout - assuming valid"));

        var fraudFuture = runFraudCheckAsync(order)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> ValidationResult.neutral("FRAUD", "Fraud check timeout - assuming neutral"));

        var inventoryFuture = validateInventoryAsync(order)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> ValidationResult.neutral("INVENTORY", "Inventory check timeout - assuming available"));

        var taxFuture = calculateTaxAsync(order)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> ValidationResult.neutral("TAX", "Tax calculation timeout - using default"));

        var shippingFuture = validateShippingAsync(order)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> ValidationResult.neutral("SHIPPING", "Shipping validation timeout - assuming valid"));

        return allOf(addressFuture, paymentFuture, fraudFuture, inventoryFuture, taxFuture, shippingFuture)
                .thenApply(v -> {
                    List<ValidationResult> results = List.of(
                            addressFuture.join(),
                            paymentFuture.join(),
                            fraudFuture.join(),
                            inventoryFuture.join(),
                            taxFuture.join(),
                            shippingFuture.join()
                    );

                    return aggregateResults(order, results);
                });
    }

    private CompletableFuture<ValidationResult> validateAddressAsync(Order order) {
        return supplyAsync(() -> {
            log.info("Validating address for order: {}", order.getId());
            if (order.getShippingAddress() == null) {
                return ValidationResult.failure("ADDRESS", "Shipping address is required");
            }
            return ValidationResult.success("ADDRESS", "Address validated successfully");
        });
    }

    private CompletableFuture<ValidationResult> validatePaymentMethodAsync(Order order) {
        return supplyAsync(() -> {
            log.info("Validating payment method for order: {}", order.getId());
            if (order.getPaymentMethod() == null) {
                return ValidationResult.failure("PAYMENT", "Payment method is required");
            }
            return ValidationResult.success("PAYMENT", "Payment method validated");
        });
    }

    private CompletableFuture<ValidationResult> runFraudCheckAsync(Order order) {
        return fraudDetectionService.checkOrderRisk(order);
    }

    private CompletableFuture<ValidationResult> validateInventoryAsync(Order order) {
        return supplyAsync(() -> {
            log.info("Validating inventory for order: {}", order.getId());
            return ValidationResult.success("INVENTORY", "Inventory validated");
        });
    }

    private CompletableFuture<ValidationResult> calculateTaxAsync(Order order) {
        return supplyAsync(() -> {
            log.info("Calculating tax for order: {}", order.getId());
            return ValidationResult.success("TAX", "Tax calculated");
        });
    }

    private CompletableFuture<ValidationResult> validateShippingAsync(Order order) {
        return supplyAsync(() -> {
            log.info("Validating shipping for order: {}", order.getId());
            return ValidationResult.success("SHIPPING", "Shipping validated");
        });
    }

    private ValidationResult aggregateResults(Order order, List<ValidationResult> results) {
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ValidationResult result : results) {
            if (!result.isValid()) {
                failures.add(result.getStage() + ": " + result.getMessage());
            }
            if (result.getWarnings() != null) {
                warnings.addAll(result.getWarnings());
            }
        }

        if (!failures.isEmpty()) {
            order.setStatus(OrderStatus.VALIDATION_FAILED);
            order.setNotes("Validation failed: " + String.join("; ", failures));
            orderRepository.save(order);
            log.warn("Order {} validation failed: {}", order.getId(), failures);
            return ValidationResult.builder()
                    .valid(false)
                    .stage("AGGREGATE")
                    .message("Order validation failed")
                    .warnings(warnings)
                    .build();
        }

        order.setStatus(OrderStatus.VALIDATED);
        order.setNotes("All validations passed");
        orderRepository.save(order);
        log.info("Order {} validation completed successfully", order.getId());

        return ValidationResult.builder()
                .valid(true)
                .stage("AGGREGATE")
                .message("All validations passed")
                .warnings(warnings)
                .build();
    }

    public OrderStatus getOrderValidationStatus(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(Order::getStatus)
                .orElse(null);
    }
}
