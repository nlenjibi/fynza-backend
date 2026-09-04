package ecommerce.modules.notification.async;

import ecommerce.common.config.AsyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiChannelNotificationService {

    private static final int MAX_RETRIES = 3;
    private final AsyncProperties asyncProperties;

    @Async("notificationExecutor")
    public CompletableFuture<List<NotificationResult>> dispatchMultiChannel(
            UUID userId, String eventType, String message) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Sending multi-channel notifications for user: {}, event: {}", 
                correlationId, userId, eventType);

        long timeout = asyncProperties.getTimeouts().getNotification();

        var emailFuture = sendEmailAsync(userId, message)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> NotificationResult.failure("EMAIL", ex.getMessage(), 0));

        var smsFuture = sendSmsAsync(userId, message)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> NotificationResult.failure("SMS", ex.getMessage(), 0));

        var pushFuture = sendPushAsync(userId, message)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> NotificationResult.failure("PUSH", ex.getMessage(), 0));

        return allOf(emailFuture, smsFuture, pushFuture)
                .thenApply(v -> List.of(
                        emailFuture.join(),
                        smsFuture.join(),
                        pushFuture.join()
                ))
                .thenApply(results -> {
                    log.info("[{}] Multi-channel notification completed. Success: {}/{}", 
                            correlationId, 
                            results.stream().filter(NotificationResult::isSuccess).count(),
                            results.size());
                    return results;
                });
    }

    private CompletableFuture<NotificationResult> sendEmailAsync(UUID userId, String message) {
        return supplyAsync(() -> {
            log.debug("Sending email to user: {}", userId);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return NotificationResult.success("EMAIL");
        });
    }

    private CompletableFuture<NotificationResult> sendSmsAsync(UUID userId, String message) {
        return supplyAsync(() -> {
            log.debug("Sending SMS to user: {}", userId);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return NotificationResult.success("SMS");
        });
    }

    private CompletableFuture<NotificationResult> sendPushAsync(UUID userId, String message) {
        return supplyAsync(() -> {
            log.debug("Sending push notification to user: {}", userId);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return NotificationResult.success("PUSH");
        });
    }

    public CompletableFuture<NotificationResult> sendWithRetry(
            String channel, UUID userId, String message) {
        
        return retryableSend(channel, userId, message, 0);
    }

    private CompletableFuture<NotificationResult> retryableSend(
            String channel, UUID userId, String message, int attempt) {
        
        if (attempt >= MAX_RETRIES) {
            return CompletableFuture.completedFuture(
                    NotificationResult.failure(channel, "Max retries exceeded", attempt));
        }

        return switch (channel.toUpperCase()) {
            case "EMAIL" -> sendEmailAsync(userId, message);
            case "SMS" -> sendSmsAsync(userId, message);
            case "PUSH" -> sendPushAsync(userId, message);
            default -> CompletableFuture.completedFuture(
                    NotificationResult.failure(channel, "Unknown channel", attempt));
        };
    }
}
