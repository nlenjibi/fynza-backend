package ecommerce.modules.order.async;

import ecommerce.modules.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private static final double HIGH_RISK_THRESHOLD = 0.7;
    private static final double MEDIUM_RISK_THRESHOLD = 0.4;

    public CompletableFuture<ValidationResult> checkOrderRisk(Order order) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Running fraud detection for order: {}", correlationId, order.getId());

        var creditCheckFuture = checkCredit(order)
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> FraudResult.neutral("Credit check timeout"));

        var addressVerificationFuture = verifyAddress(order)
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> FraudResult.neutral("Address verification timeout"));

        var deviceFingerprintFuture = checkDevice(order)
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> FraudResult.neutral("Device check timeout"));

        return CompletableFuture.allOf(creditCheckFuture, addressVerificationFuture, deviceFingerprintFuture)
                .thenApply(v -> {
                    FraudResult creditResult = creditCheckFuture.join();
                    FraudResult addressResult = addressVerificationFuture.join();
                    FraudResult deviceResult = deviceFingerprintFuture.join();
                    
                    FraudResult firstApproved = creditResult.isApproved() ? creditResult : 
                            (addressResult.isApproved() ? addressResult : 
                            (deviceResult.isApproved() ? deviceResult : null));
                    
                    if (firstApproved != null) {
                        log.info("[{}] Fraud check approved for order: {}", correlationId, order.getId());
                        return ValidationResult.success("FRAUD", "Risk assessment: LOW");
                    }
                    return ValidationResult.neutral("FRAUD", "All checks neutral");
                })
                .exceptionally(ex -> {
                    log.error("[{}] Error in fraud detection for order: {}", correlationId, order.getId(), ex);
                    return ValidationResult.neutral("FRAUD", "Fraud check error - assuming neutral");
                });
    }

    private CompletableFuture<FraudResult> checkCredit(Order order) {
        return supplyAsync(() -> {
            log.debug("Running credit check for order: {}", order.getId());
            double riskScore = Math.random() * 0.3;
            if (riskScore < MEDIUM_RISK_THRESHOLD) {
                return FraudResult.approved("Credit check passed");
            }
            return FraudResult.neutral("Credit check inconclusive");
        });
    }

    private CompletableFuture<FraudResult> verifyAddress(Order order) {
        return supplyAsync(() -> {
            log.debug("Verifying address for order: {}", order.getId());
            double riskScore = Math.random() * 0.2;
            if (riskScore < MEDIUM_RISK_THRESHOLD) {
                return FraudResult.approved("Address verified");
            }
            return FraudResult.neutral("Address verification inconclusive");
        });
    }

    private CompletableFuture<FraudResult> checkDevice(Order order) {
        return supplyAsync(() -> {
            log.debug("Checking device fingerprint for order: {}", order.getId());
            double riskScore = Math.random() * 0.25;
            if (riskScore < MEDIUM_RISK_THRESHOLD) {
                return FraudResult.approved("Device check passed");
            }
            return FraudResult.neutral("Device check inconclusive");
        });
    }

    private ValidationResult handleConsensusRejection(
            CompletableFuture<FraudResult> creditCheck,
            CompletableFuture<FraudResult> addressVerification,
            CompletableFuture<FraudResult> deviceFingerprint) {

        return allOf(creditCheck, addressVerification, deviceFingerprint)
                .thenApply(v -> {
                    boolean allReject = creditCheck.join().isRejected() &&
                            addressVerification.join().isRejected() &&
                            deviceFingerprint.join().isRejected();

                    if (allReject) {
                        log.warn("All fraud services consensus - rejecting order");
                        return ValidationResult.failure("FRAUD", "High fraud risk detected - manual review required",
                                Map.of("riskLevel", "HIGH", "action", "MANUAL_REVIEW"));
                    }
                    return ValidationResult.success("FRAUD", "Risk assessment: ACCEPTABLE");
                }).join();
    }

    public static class FraudResult {
        private final boolean approved;
        private final boolean rejected;
        private final String message;

        private FraudResult(boolean approved, boolean rejected, String message) {
            this.approved = approved;
            this.rejected = rejected;
            this.message = message;
        }

        public static FraudResult approved(String message) {
            return new FraudResult(true, false, message);
        }

        public static FraudResult rejected(String message) {
            return new FraudResult(false, true, message);
        }

        public static FraudResult neutral(String message) {
            return new FraudResult(false, false, message);
        }

        public boolean isApproved() { return approved; }
        public boolean isRejected() { return rejected; }
        public String getMessage() { return message; }
    }
}
