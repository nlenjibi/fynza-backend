package ecommerce.modules.notification.service.impl;

import ecommerce.modules.notification.dto.EmailRequest;
import ecommerce.modules.notification.dto.EntityRef;
import ecommerce.modules.notification.dto.NotificationResponse;
import ecommerce.modules.notification.entity.Notification;
import ecommerce.modules.notification.entity.NotificationChannelConfig;
import ecommerce.modules.notification.entity.NotificationDispatch;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationStatus;
import ecommerce.modules.notification.enums.NotificationType;
import ecommerce.modules.notification.exceptions.EmailDispatchException;
import ecommerce.modules.notification.exceptions.SlackDispatchException;
import ecommerce.modules.notification.provider.EmailProvider;
import ecommerce.modules.notification.provider.SlackClient;
import ecommerce.modules.notification.repository.*;
import ecommerce.modules.notification.service.NotificationService;
import ecommerce.modules.notification.service.SlackChannelResolver;
import ecommerce.modules.notification.service.TemplateInterpolator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository             notificationRepo;
    private final NotificationDispatchRepository     dispatchRepo;
    private final NotificationTemplateRepository     templateRepo;
    private final NotificationChannelConfigRepository channelConfigRepo;
    private final NotificationPreferenceRepository   preferenceRepo;
    private final EmailProvider                      emailProvider;
    private final SlackClient                        slackClient;
    private final SlackChannelResolver               slackChannelResolver;
    private final TemplateInterpolator               interpolator;
    private final SimpMessagingTemplate              messagingTemplate;

    @Value("${fynza.notification.email.from}")
    private String fromAddress;

    @Value("${fynza.notifications-enabled:true}")
    private boolean notificationsEnabled;

    public NotificationServiceImpl(
            NotificationRepository notificationRepo,
            NotificationDispatchRepository dispatchRepo,
            NotificationTemplateRepository templateRepo,
            NotificationChannelConfigRepository channelConfigRepo,
            NotificationPreferenceRepository preferenceRepo,
            EmailProvider emailProvider,
            SlackClient slackClient,
            SlackChannelResolver slackChannelResolver,
            TemplateInterpolator interpolator,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.notificationRepo     = notificationRepo;
        this.dispatchRepo         = dispatchRepo;
        this.templateRepo         = templateRepo;
        this.channelConfigRepo    = channelConfigRepo;
        this.preferenceRepo       = preferenceRepo;
        this.emailProvider        = emailProvider;
        this.slackClient          = slackClient;
        this.slackChannelResolver = slackChannelResolver;
        this.interpolator         = interpolator;
        this.messagingTemplate    = messagingTemplate;
    }

    // ── Send ─────────────────────────────────────────────────────────────────

    @Override
    @Async("notificationTaskExecutor")
    public void send(NotificationType type,
                     UUID recipientId,
                     UUID sellerId,
                     Map<String, String> variables,
                     String deepLink,
                     EntityRef entity) {

        if (!notificationsEnabled) {
            log.debug("[Notification] Disabled — skipping type={}", type);
            return;
        }

        UUID eventId = UUID.randomUUID();

        NotificationChannelConfig config = channelConfigRepo
                .findByNotificationType(type)
                .orElseGet(() -> defaultConfig(type));

        if (config.isInAppEnabled() && isChannelEnabled(recipientId, type, NotificationChannel.IN_APP)) {
            saveInAppNotification(type, recipientId, sellerId, variables, deepLink, entity);
        }

        if (config.isEmailEnabled() && isChannelEnabled(recipientId, type, NotificationChannel.EMAIL)) {
            dispatchEmail(type, recipientId, variables, eventId, config);
        }
    }

    @Override
    @Async("notificationTaskExecutor")
    public void sendToExternalRecipient(NotificationType type,
                                        String recipientEmail,
                                        Map<String, String> variables) {
        if (!notificationsEnabled) return;
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[Notification] sendToExternalRecipient called with no recipientEmail type={}", type);
            return;
        }

        var template = templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.EMAIL).orElse(null);
        if (template == null) {
            log.warn("[Notification] No EMAIL template for external type={}", type);
            return;
        }

        var config = channelConfigRepo.findByNotificationType(type).orElseGet(() -> defaultConfig(type));
        if (!config.isEmailEnabled()) return;

        String subject  = interpolator.interpolate(template.getSubject(), variables);
        String textBody = interpolator.interpolate(template.getBody(), variables);
        String htmlBody = template.getHtmlBody() != null ? interpolator.interpolate(template.getHtmlBody(), variables) : null;

        EmailRequest emailRequest = EmailRequest.builder()
                .from(fromAddress).to(List.of(recipientEmail))
                .subject(subject).textBody(textBody).htmlBody(htmlBody)
                .tags(Map.of("notificationType", type.name()))
                .build();

        NotificationDispatch dispatch = dispatchRepo.save(NotificationDispatch.builder()
                .recipientEmail(recipientEmail)
                .notificationEventId(UUID.randomUUID())
                .notificationType(type).channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING).subject(subject).textBody(textBody)
                .providerName(emailProvider.providerName())
                .build());

        sendEmailAttempt(dispatch, emailRequest, type, config);
    }

    @Override
    @Async("notificationTaskExecutor")
    public void sendBroadcast(NotificationType type,
                              UUID sourceEntityId,
                              UUID sellerId,
                              Map<String, String> variables) {
        if (!notificationsEnabled) return;
        try {
            var config = channelConfigRepo.findByNotificationType(type).orElseGet(() -> defaultConfig(type));
            if (!config.isSlackEnabled()) return;

            var template = templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.SLACK).orElse(null);
            if (template == null) {
                log.warn("[Notification] No SLACK template for type={}", type);
                return;
            }
            if (!slackClient.isConfigured()) return;

            if (dispatchRepo.existsByChannelAndNotificationTypeAndSourceEntityId(
                    NotificationChannel.SLACK, type, sourceEntityId)) {
                log.debug("[Notification] SLACK broadcast already dispatched type={} — skipping duplicate", type);
                return;
            }

            var channelId = slackChannelResolver.resolve(sellerId);
            if (channelId.isEmpty()) {
                log.warn("[Notification] No Slack channel resolved sellerId={} type={}", sellerId, type);
                return;
            }

            String text = interpolator.interpolate(template.getBody(), variables);

            NotificationDispatch dispatch = dispatchRepo.save(NotificationDispatch.builder()
                    .sourceEntityId(sourceEntityId).notificationEventId(UUID.randomUUID())
                    .notificationType(type).channel(NotificationChannel.SLACK)
                    .status(NotificationStatus.PENDING).textBody(text)
                    .providerName("SLACK_BOT").slackChannelId(channelId.get())
                    .build());

            attemptSlackSend(dispatch, channelId.get(), type);
        } catch (Exception ex) {
            log.error("[Notification] SLACK broadcast failed type={}: {}", type, ex.getMessage(), ex);
        }
    }

    // ── Query ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID publicId, UUID recipientId) {
        return notificationRepo.findByPublicIdAndRecipientId(publicId, recipientId)
                .map(NotificationResponse::from)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Notification not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForRecipient(UUID recipientId, Pageable pageable) {
        return notificationRepo.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID recipientId) {
        return notificationRepo.countUnreadByRecipientId(recipientId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID publicId, UUID recipientId) {
        int updated = notificationRepo.markAsRead(publicId, recipientId, Instant.now());
        if (updated == 0) throw new jakarta.persistence.EntityNotFoundException("Notification not found.");
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepo.markAllReadForRecipient(recipientId, Instant.now());
    }

    @Override
    @Transactional
    public void softDelete(UUID publicId, UUID recipientId) {
        int updated = notificationRepo.softDelete(publicId, recipientId, Instant.now());
        if (updated == 0) throw new jakarta.persistence.EntityNotFoundException("Notification not found.");
    }

    @Override
    @Transactional
    public void softDeleteAll(UUID recipientId) {
        notificationRepo.softDeleteAllForRecipient(recipientId, Instant.now());
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void saveInAppNotification(NotificationType type, UUID recipientId, UUID sellerId,
                                       Map<String, String> variables, String deepLink, EntityRef entity) {
        templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.IN_APP)
                .ifPresentOrElse(template -> {
                    String title = interpolator.interpolate(template.getSubject(), variables);
                    String body  = interpolator.interpolate(template.getBody(), variables);
                    Notification saved = notificationRepo.save(Notification.builder()
                            .recipientId(recipientId).sellerId(sellerId)
                            .notificationType(type).title(title).body(body).deepLink(deepLink)
                            .entityType(entity != null ? entity.type() : null)
                            .entityId(entity != null ? entity.id() : null)
                            .build());
                    pushToWebSocket(recipientId, saved);
                    log.debug("[Notification] IN_APP saved type={} recipient={}", type, recipientId);
                }, () -> log.warn("[Notification] No IN_APP template for type={}", type));
    }

    private void dispatchEmail(NotificationType type, UUID recipientId,
                               Map<String, String> variables, UUID eventId,
                               NotificationChannelConfig config) {
        var template = templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.EMAIL).orElse(null);
        if (template == null) {
            log.warn("[Notification] No EMAIL template for type={}", type);
            return;
        }

        String recipientEmail = variables.get("recipientEmail");
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[Notification] recipientEmail missing for type={} recipient={}", type, recipientId);
            return;
        }

        String subject  = interpolator.interpolate(template.getSubject(), variables);
        String textBody = interpolator.interpolate(template.getBody(), variables);
        String htmlBody = template.getHtmlBody() != null ? interpolator.interpolate(template.getHtmlBody(), variables) : null;

        EmailRequest emailRequest = EmailRequest.builder()
                .from(fromAddress).to(List.of(recipientEmail))
                .subject(subject).textBody(textBody).htmlBody(htmlBody)
                .tags(Map.of("notificationType", type.name()))
                .build();

        NotificationDispatch dispatch = dispatchRepo.save(NotificationDispatch.builder()
                .recipientId(recipientId).recipientEmail(recipientEmail)
                .notificationEventId(eventId).notificationType(type)
                .channel(NotificationChannel.EMAIL).status(NotificationStatus.PENDING)
                .subject(subject).textBody(textBody).providerName(emailProvider.providerName())
                .build());

        sendEmailAttempt(dispatch, emailRequest, type, config);
    }

    private void sendEmailAttempt(NotificationDispatch dispatch, EmailRequest emailRequest,
                                  NotificationType type, NotificationChannelConfig config) {
        try {
            dispatch.setStatus(NotificationStatus.SENDING);
            dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
            dispatchRepo.save(dispatch);

            String messageId = emailProvider.send(emailRequest);

            dispatch.setStatus(NotificationStatus.SENT);
            dispatch.setProviderMessageId(messageId);
            dispatch.setSentAt(Instant.now());
            dispatchRepo.save(dispatch);

            log.info("[Notification] EMAIL sent type={} to={}", type, emailRequest.getTo());

        } catch (EmailDispatchException ex) {
            dispatch.setFailureReason(ex.getMessage());
            boolean canRetry = ex.isRetryable() && dispatch.getAttemptCount() < config.getMaxRetries();
            if (canRetry) {
                long delay = config.getRetryDelaySeconds() * (long) Math.pow(2, (double) dispatch.getAttemptCount() - 1);
                dispatch.setStatus(NotificationStatus.PENDING);
                dispatch.setScheduledAt(Instant.now().plusSeconds(delay));
                log.warn("[Notification] EMAIL failed (retry in {}s) type={}", delay, type);
            } else {
                dispatch.setStatus(NotificationStatus.FAILED);
                log.error("[Notification] EMAIL permanently failed type={}", type);
            }
            dispatchRepo.save(dispatch);
        } catch (Exception ex) {
            dispatch.setStatus(NotificationStatus.FAILED);
            dispatch.setFailureReason("Unexpected: " + ex.getMessage());
            dispatchRepo.save(dispatch);
            log.error("[Notification] EMAIL unexpected error type={}", type, ex);
        }
    }

    private void attemptSlackSend(NotificationDispatch dispatch, String channelId, NotificationType type) {
        try {
            dispatch.setStatus(NotificationStatus.SENDING);
            dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
            dispatchRepo.save(dispatch);

            slackClient.send(channelId, dispatch.getTextBody());

            dispatch.setStatus(NotificationStatus.SENT);
            dispatch.setSentAt(Instant.now());
            dispatchRepo.save(dispatch);

            log.info("[Notification] SLACK sent type={}", type);

        } catch (SlackDispatchException ex) {
            dispatch.setFailureReason(ex.getMessage());
            dispatch.setStatus(ex.isRetryable() ? NotificationStatus.PENDING : NotificationStatus.FAILED);
            if (ex.isRetryable()) dispatch.setScheduledAt(Instant.now().plusSeconds(60));
            dispatchRepo.save(dispatch);
        }
    }

    private void pushToWebSocket(UUID recipientId, Notification notification) {
        if (messagingTemplate == null) return;
        try {
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    "/queue/notifications",
                    NotificationResponse.from(notification));
        } catch (Exception ex) {
            log.warn("[Notification] WS push failed recipient={}", recipientId, ex);
        }
    }

    private boolean isChannelEnabled(UUID userId, NotificationType type, NotificationChannel channel) {
        return preferenceRepo
                .findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
                .map(p -> p.isEnabled())
                .orElse(true);
    }

    private NotificationChannelConfig defaultConfig(NotificationType type) {
        return NotificationChannelConfig.builder()
                .notificationType(type).emailEnabled(true).inAppEnabled(true)
                .maxRetries(3).retryDelaySeconds(60).build();
    }
}
