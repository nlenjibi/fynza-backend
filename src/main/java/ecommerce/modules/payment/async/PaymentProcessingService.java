package ecommerce.modules.payment.async;

import ecommerce.common.config.AsyncProperties;
import ecommerce.modules.payment.entity.PaymentTransaction;
import ecommerce.modules.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static ecommerce.modules.payment.async.PaymentResult.PaymentStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingService {

    private final PaymentTransactionRepository paymentRepository;
    private final AsyncProperties asyncProperties;

    @Async("paymentExecutor")
    public CompletableFuture<PaymentResult> processPaymentAsync(UUID transactionId) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Starting async payment processing for transaction: {}", correlationId, transactionId);

        long timeout = asyncProperties.getTimeouts().getPayment();

        PaymentTransaction transaction = paymentRepository.findByPublicId(transactionId).orElse(null);
        if (transaction == null) {
            return CompletableFuture.completedFuture(
                    PaymentResult.failed(transactionId, "Transaction not found"));
        }

        transaction.setStatus(ecommerce.common.enums.PaymentStatus.PROCESSING);
        paymentRepository.save(transaction);

        return tokenizePayment(transaction)
                .thenComposeAsync(this::callPaymentGateway, java.util.concurrent.Executors.newCachedThreadPool())
                .thenComposeAsync(this::process3DSecure, java.util.concurrent.Executors.newCachedThreadPool())
                .thenComposeAsync(this::captureFunds, java.util.concurrent.Executors.newCachedThreadPool())
                .thenApply(this::updateOrderStatus)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> handlePaymentFailure(transaction, ex));
    }

    private CompletableFuture<String> tokenizePayment(PaymentTransaction transaction) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Tokenizing payment for transaction: {}", transaction.getId());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "tok_" + UUID.randomUUID().toString().replace("-", "");
        });
    }

    private CompletableFuture<String> callPaymentGateway(String token) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Calling payment gateway with token: {}", token);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "auth_" + UUID.randomUUID().toString().substring(0, 8);
        });
    }

    private CompletableFuture<String> process3DSecure(String authCode) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Processing 3D Secure for auth: {}", authCode);
            return authCode;
        });
    }

    private CompletableFuture<PaymentResult> captureFunds(String authCode) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Capturing funds for auth: {}", authCode);
            return PaymentResult.success(
                    UUID.randomUUID(),
                    "PRIMARY_GATEWAY",
                    BigDecimal.valueOf(100.00),
                    authCode
            );
        });
    }

    private PaymentResult updateOrderStatus(PaymentResult result) {
        log.info("Payment completed with status: {}", result.getStatus());
        return result;
    }

    private PaymentResult handlePaymentFailure(PaymentTransaction transaction, Throwable ex) {
        log.error("Payment processing failed for transaction: {}", transaction.getId(), ex);
        transaction.setStatus(ecommerce.common.enums.PaymentStatus.FAILED);
        transaction.setFailureReason(ex.getMessage());
        paymentRepository.save(transaction);

        return PaymentResult.failed(transaction.getPublicId(), "Payment failed: " + ex.getMessage());
    }

    public CompletableFuture<PaymentResult> processWithGatewayFallback(UUID transactionId) {
        var primaryFuture = processPaymentAsync(transactionId);
        var fallbackFuture = processWithBackupGateway(transactionId);

        return CompletableFuture.anyOf(primaryFuture, fallbackFuture)
                .thenApply(result -> (PaymentResult) result)
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> PaymentResult.failed(transactionId, "All payment gateways failed"));
    }

    private CompletableFuture<PaymentResult> processWithBackupGateway(UUID transactionId) {
        log.info("Processing payment with backup gateway for transaction: {}", transactionId);
        return CompletableFuture.supplyAsync(() -> 
                PaymentResult.success(
                        transactionId,
                        "BACKUP_GATEWAY",
                        BigDecimal.valueOf(100.00),
                        "backup_" + UUID.randomUUID().toString().substring(0, 8)
                )
        );
    }
}
