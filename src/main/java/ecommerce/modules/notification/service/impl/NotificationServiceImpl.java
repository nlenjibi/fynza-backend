package ecommerce.modules.notification.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.common.cache.CacheService;
import ecommerce.modules.notification.dto.EmailRequest;
import ecommerce.modules.notification.dto.EntityRef;
import ecommerce.modules.notification.dto.NotificationBadgePayload;
import ecommerce.modules.notification.dto.NotificationResponse;
import ecommerce.modules.notification.dto.QuietHoursResponse;
import ecommerce.modules.notification.entity.Notification;
import ecommerce.modules.notification.entity.NotificationChannelConfig;
import ecommerce.modules.notification.entity.NotificationDevice;
import ecommerce.modules.notification.entity.NotificationDispatch;
import ecommerce.modules.notification.entity.NotificationPreference;
import ecommerce.modules.notification.entity.NotificationQuietHours;
import ecommerce.modules.notification.enums.DeviceStatus;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationStatus;
import ecommerce.modules.notification.enums.NotificationType;
import ecommerce.modules.notification.exceptions.EmailDispatchException;
import ecommerce.modules.notification.exceptions.SlackDispatchException;
import ecommerce.modules.notification.provider.EmailProvider;
import ecommerce.modules.notification.provider.PushProvider;
import ecommerce.modules.notification.provider.SlackClient;
import ecommerce.modules.notification.provider.SmsProvider;
import ecommerce.modules.notification.repository.NotificationChannelConfigRepository;
import ecommerce.modules.notification.repository.NotificationDeviceRepository;
import ecommerce.modules.notification.repository.NotificationDispatchRepository;
import ecommerce.modules.notification.repository.NotificationPreferenceRepository;
import ecommerce.modules.notification.repository.NotificationQuietHoursRepository;
import ecommerce.modules.notification.repository.NotificationRepository;
import ecommerce.modules.notification.repository.NotificationTemplateRepository;
import ecommerce.modules.notification.service.NotificationService;
import ecommerce.modules.notification.service.SlackChannelResolver;
import ecommerce.modules.notification.service.TemplateInterpolator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Duration UNREAD_COUNT_TTL = Duration.ofMinutes(5);
    private static final Duration PREFERENCE_TTL   = Duration.ofMinutes(30);

    private final NotificationRepository              notificationRepo;
    private final NotificationDispatchRepository      dispatchRepo;
    private final NotificationTemplateRepository      templateRepo;
    private final NotificationChannelConfigRepository channelConfigRepo;
    private final NotificationPreferenceRepository    preferenceRepo;
    private final EmailProvider                       emailProvider;
    private final SmsProvider                         smsProvider;
    private final PushProvider                        pushProvider;
    private final NotificationDeviceRepository        deviceRepository;
    private final SlackClient                         slackClient;
    private final SlackChannelResolver                slackChannelResolver;
    private final TemplateInterpolator                interpolator;
    private final SimpMessagingTemplate               messagingTemplate;
    private final CacheService                        cacheService;
    private final MeterRegistry                       meterRegistry;
    private final NotificationQuietHoursRepository    quietHoursRepo;

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
            SmsProvider smsProvider,
            PushProvider pushProvider,
            NotificationDeviceRepository deviceRepository,
            SlackClient slackClient,
            SlackChannelResolver slackChannelResolver,
            TemplateInterpolator interpolator,
            @Lazy SimpMessagingTemplate messagingTemplate,
            CacheService cacheService,
            MeterRegistry meterRegistry,
            NotificationQuietHoursRepository quietHoursRepo) {
        this.notificationRepo     = notificationRepo;
        this.dispatchRepo         = dispatchRepo;
        this.templateRepo         = templateRepo;
        this.channelConfigRepo    = channelConfigRepo;
        this.preferenceRepo       = preferenceRepo;
        this.emailProvider        = emailProvider;
        this.smsProvider          = smsProvider;
        this.pushProvider         = pushProvider;
        this.deviceRepository     = deviceRepository;
        this.slackClient          = slackClient;
        this.slackChannelResolver = slackChannelResolver;
        this.interpolator         = interpolator;
        this.messagingTemplate    = messagingTemplate;
        this.cacheService         = cacheService;
        this.meterRegistry        = meterRegistry;
        this.quietHoursRepo       = quietHoursRepo;
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

        boolean quietHoursActive = recipientId != null && isQuietHoursActive(recipientId);

        if (config.isInAppEnabled() && isChannelEnabled(recipientId, type, NotificationChannel.IN_APP)) {
            saveInAppNotification(type, recipientId, sellerId, variables, deepLink, entity);
        }

        if (config.isEmailEnabled() && isChannelEnabled(recipientId, type, NotificationChannel.EMAIL)) {
            dispatchEmail(type, recipientId, variables, eventId, config);
        }

        if (config.isSmsEnabled() && isChannelEnabled(recipientId, type, NotificationChannel.SMS)) {
            if (quietHoursActive) {
                meterRegistry.counter("fynza.notification.suppressed",
                        "type", type.name(), "reason", "QUIET_HOURS", "channel", "SMS").increment();
                log.debug("[Notification] SMS suppressed (quiet hours) type={} recipient={}", type, recipientId);
            } else {
                dispatchSms(type, recipientId, variables, eventId);
            }
        }

        if (config.isPushEnabled() && isChannelEnabled(recipientId, type, NotificationChannel.PUSH)) {
            if (quietHoursActive) {
                meterRegistry.counter("fynza.notification.suppressed",
                        "type", type.name(), "reason", "QUIET_HOURS", "channel", "PUSH").increment();
                log.debug("[Notification] PUSH suppressed (quiet hours) type={} recipient={}", type, recipientId);
            } else {
                dispatchPush(type, recipientId, variables, eventId);
            }
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
        String htmlBody = template.getHtmlBody() != null
                ? interpolator.interpolate(template.getHtmlBody(), variables)
                : null;

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
        String key = CacheNames.NOTIFICATION_UNREAD_COUNT + ":" + recipientId;
        return cacheService.get(key, String.class)
                .map(Long::parseLong)
                .orElseGet(() -> {
                    long count = notificationRepo.countUnreadByRecipientId(recipientId);
                    cacheService.put(key, String.valueOf(count), UNREAD_COUNT_TTL);
                    return count;
                });
    }

    @Override
    @Transactional
    public void markAsRead(UUID publicId, UUID recipientId) {
        int updated = notificationRepo.markAsRead(publicId, recipientId, Instant.now());
        if (updated == 0) throw new jakarta.persistence.EntityNotFoundException("Notification not found.");
        evictUnreadCountCache(recipientId);
        pushBadgeUpdate(recipientId);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepo.markAllReadForRecipient(recipientId, Instant.now());
        evictUnreadCountCache(recipientId);
        pushBadgeUpdate(recipientId);
    }

    @Override
    @Transactional
    public void softDelete(UUID publicId, UUID recipientId) {
        int updated = notificationRepo.softDelete(publicId, recipientId, Instant.now());
        if (updated == 0) throw new jakarta.persistence.EntityNotFoundException("Notification not found.");
        evictUnreadCountCache(recipientId);
        pushBadgeUpdate(recipientId);
    }

    @Override
    @Transactional
    public void softDeleteAll(UUID recipientId) {
        notificationRepo.softDeleteAllForRecipient(recipientId, Instant.now());
        evictUnreadCountCache(recipientId);
        pushBadgeUpdate(recipientId);
    }

    // ── Quiet Hours ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<QuietHoursResponse> getQuietHours(UUID userId) {
        return quietHoursRepo.findByUserId(userId).map(QuietHoursResponse::from);
    }

    @Override
    @Transactional
    public QuietHoursResponse updateQuietHours(UUID userId, String startTime, String endTime, String timezone) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end   = LocalTime.parse(endTime);
        ZoneId.of(timezone); // validates — throws ZoneRulesException if invalid
        NotificationQuietHours qh = quietHoursRepo.findByUserId(userId)
                .orElseGet(() -> NotificationQuietHours.builder().userId(userId).build());
        qh.setStartTime(start);
        qh.setEndTime(end);
        qh.setTimezone(timezone);
        return QuietHoursResponse.from(quietHoursRepo.save(qh));
    }

    @Override
    @Transactional
    public void deleteQuietHours(UUID userId) {
        quietHoursRepo.findByUserId(userId).ifPresent(quietHoursRepo::delete);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void saveInAppNotification(NotificationType type, UUID recipientId, UUID sellerId,
                                       Map<String, String> variables, String deepLink, EntityRef entity) {
        templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.IN_APP)
                .ifPresentOrElse(template -> {
                    String idempotencyKey = type.name() + ":"
                            + (entity != null ? entity.type() : "NONE") + ":"
                            + (entity != null ? entity.id() : "NONE") + ":"
                            + recipientId;

                    if (notificationRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
                        log.debug("[Notification] Duplicate IN_APP suppressed type={} key={}", type, idempotencyKey);
                        meterRegistry.counter("fynza.notification.suppressed",
                                "type", type.name(), "reason", "DUPLICATE", "channel", "IN_APP").increment();
                        return;
                    }

                    String title = interpolator.interpolate(template.getSubject(), variables);
                    String body  = interpolator.interpolate(template.getBody(), variables);
                    Notification saved = notificationRepo.save(Notification.builder()
                            .recipientId(recipientId).sellerId(sellerId)
                            .notificationType(type).title(title).body(body).deepLink(deepLink)
                            .entityType(entity != null ? entity.type() : null)
                            .entityId(entity != null ? entity.id() : null)
                            .idempotencyKey(idempotencyKey)
                            .build());

                    meterRegistry.counter("fynza.notification.created",
                            "type", type.name(), "channel", "IN_APP").increment();
                    evictUnreadCountCache(recipientId);
                    pushToWebSocket(recipientId, saved);
                    pushBadgeUpdate(recipientId);
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
        String htmlBody = template.getHtmlBody() != null
                ? interpolator.interpolate(template.getHtmlBody(), variables)
                : null;

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

            meterRegistry.counter("fynza.notification.sent", "type", type.name(), "channel", "EMAIL").increment();
            log.info("[Notification] EMAIL sent type={} to={}", type, emailRequest.getTo());

        } catch (EmailDispatchException ex) {
            dispatch.setFailureReason(ex.getMessage());
            boolean canRetry = ex.isRetryable() && dispatch.getAttemptCount() < config.getMaxRetries();
            if (canRetry) {
                long delay = config.getRetryDelaySeconds()
                        * (long) Math.pow(2, (double) dispatch.getAttemptCount() - 1);
                dispatch.setStatus(NotificationStatus.PENDING);
                dispatch.setScheduledAt(Instant.now().plusSeconds(delay));
                log.warn("[Notification] EMAIL failed (retry in {}s) type={}", delay, type);
            } else {
                dispatch.setStatus(NotificationStatus.FAILED);
                meterRegistry.counter("fynza.notification.failed",
                        "type", type.name(), "channel", "EMAIL").increment();
                log.error("[Notification] EMAIL permanently failed type={}", type);
            }
            dispatchRepo.save(dispatch);
        } catch (Exception ex) {
            dispatch.setStatus(NotificationStatus.FAILED);
            dispatch.setFailureReason("Unexpected: " + ex.getMessage());
            dispatchRepo.save(dispatch);
            meterRegistry.counter("fynza.notification.failed",
                    "type", type.name(), "channel", "EMAIL").increment();
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

            meterRegistry.counter("fynza.notification.sent", "type", type.name(), "channel", "SLACK").increment();
            log.info("[Notification] SLACK sent type={}", type);

        } catch (SlackDispatchException ex) {
            dispatch.setFailureReason(ex.getMessage());
            dispatch.setStatus(ex.isRetryable() ? NotificationStatus.PENDING : NotificationStatus.FAILED);
            if (ex.isRetryable()) {
                dispatch.setScheduledAt(Instant.now().plusSeconds(60));
            } else {
                meterRegistry.counter("fynza.notification.failed",
                        "type", type.name(), "channel", "SLACK").increment();
            }
            dispatchRepo.save(dispatch);
        }
    }

    private void dispatchSms(NotificationType type, UUID recipientId,
                             Map<String, String> variables, UUID eventId) {
        var template = templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.SMS).orElse(null);
        if (template == null) {
            log.warn("[Notification] No SMS template for type={}", type);
            return;
        }
        String recipientPhone = variables.get("recipientPhone");
        if (recipientPhone == null || recipientPhone.isBlank()) {
            log.warn("[Notification] recipientPhone missing for type={} recipient={}", type, recipientId);
            return;
        }
        String body            = interpolator.interpolate(template.getBody(), variables);
        String idempotencyKey  = "SMS-" + eventId;

        NotificationDispatch dispatch = dispatchRepo.save(NotificationDispatch.builder()
                .recipientId(recipientId).notificationEventId(eventId)
                .notificationType(type).channel(NotificationChannel.SMS)
                .status(NotificationStatus.SENDING).textBody(body)
                .providerName(smsProvider.providerName()).attemptCount(1)
                .build());
        try {
            String msgId = smsProvider.send(recipientPhone, body, idempotencyKey);
            dispatch.setStatus(NotificationStatus.SENT);
            dispatch.setProviderMessageId(msgId);
            dispatch.setSentAt(Instant.now());
            meterRegistry.counter("fynza.notification.sent", "type", type.name(), "channel", "SMS").increment();
            log.info("[Notification] SMS sent type={} to={}", type, recipientPhone);
        } catch (Exception ex) {
            dispatch.setStatus(NotificationStatus.FAILED);
            dispatch.setFailureReason(ex.getMessage());
            meterRegistry.counter("fynza.notification.failed", "type", type.name(), "channel", "SMS").increment();
            log.error("[Notification] SMS failed type={}: {}", type, ex.getMessage());
        }
        dispatchRepo.save(dispatch);
    }

    private void dispatchPush(NotificationType type, UUID recipientId,
                              Map<String, String> variables, UUID eventId) {
        var template = templateRepo.findByNotificationTypeAndChannel(type, NotificationChannel.PUSH).orElse(null);
        if (template == null) {
            log.warn("[Notification] No PUSH template for type={}", type);
            return;
        }
        List<NotificationDevice> devices = deviceRepository.findByUserIdAndStatus(recipientId, DeviceStatus.ACTIVE);
        if (devices.isEmpty()) {
            log.debug("[Notification] No active push devices for recipient={}", recipientId);
            return;
        }
        String title = interpolator.interpolate(template.getSubject(), variables);
        String body  = interpolator.interpolate(template.getBody(), variables);

        for (NotificationDevice device : devices) {
            NotificationDispatch dispatch = dispatchRepo.save(NotificationDispatch.builder()
                    .recipientId(recipientId).notificationEventId(eventId)
                    .notificationType(type).channel(NotificationChannel.PUSH)
                    .status(NotificationStatus.SENDING).subject(title).textBody(body)
                    .providerName(pushProvider.providerName()).attemptCount(1)
                    .build());
            try {
                String msgId = pushProvider.send(device.getDeviceToken(), title, body, Map.of());
                dispatch.setStatus(NotificationStatus.SENT);
                dispatch.setProviderMessageId(msgId);
                dispatch.setSentAt(Instant.now());
                meterRegistry.counter("fynza.notification.sent", "type", type.name(), "channel", "PUSH").increment();
                log.info("[Notification] PUSH sent type={} device={}", type, device.getPublicId());
            } catch (Exception ex) {
                dispatch.setStatus(NotificationStatus.FAILED);
                dispatch.setFailureReason(ex.getMessage());
                meterRegistry.counter("fynza.notification.failed", "type", type.name(), "channel", "PUSH").increment();
                if (pushProvider.isInvalidToken(ex.getMessage())) {
                    deviceRepository.invalidateByToken(device.getDeviceToken());
                    log.warn("[Notification] PUSH invalid token deactivated device={}", device.getPublicId());
                } else {
                    log.error("[Notification] PUSH failed type={} device={}: {}",
                            type, device.getPublicId(), ex.getMessage());
                }
            }
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

    private void pushBadgeUpdate(UUID recipientId) {
        if (messagingTemplate == null) return;
        try {
            long unread = notificationRepo.countUnreadByRecipientId(recipientId);
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    "/queue/notifications/badge",
                    new NotificationBadgePayload(unread));
            log.debug("[Notification] Badge pushed recipient={} unread={}", recipientId, unread);
        } catch (Exception ex) {
            log.warn("[Notification] Badge WS push failed recipient={}", recipientId, ex);
        }
    }

    private boolean isChannelEnabled(UUID userId, NotificationType type, NotificationChannel channel) {
        if (userId == null) return true;
        String key = CacheNames.NOTIFICATION_PREF + ":" + userId + ":" + type.name() + ":" + channel.name();
        return cacheService.get(key, String.class)
                .map(Boolean::parseBoolean)
                .orElseGet(() -> {
                    boolean enabled = preferenceRepo
                            .findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
                            .map(NotificationPreference::isEnabled)
                            .orElse(true);
                    cacheService.put(key, String.valueOf(enabled), PREFERENCE_TTL);
                    return enabled;
                });
    }

    private boolean isQuietHoursActive(UUID userId) {
        return quietHoursRepo.findByUserId(userId)
                .map(qh -> {
                    try {
                        ZoneId zone   = ZoneId.of(qh.getTimezone());
                        LocalTime now = LocalTime.now(zone);
                        LocalTime start = qh.getStartTime();
                        LocalTime end   = qh.getEndTime();
                        if (start.isBefore(end)) {
                            return !now.isBefore(start) && now.isBefore(end);
                        } else {
                            // overnight window e.g., 22:00–08:00
                            return !now.isBefore(start) || now.isBefore(end);
                        }
                    } catch (Exception ex) {
                        log.warn("[Notification] Quiet hours check failed userId={}: {}", userId, ex.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }

    private void evictUnreadCountCache(UUID recipientId) {
        cacheService.evict(CacheNames.NOTIFICATION_UNREAD_COUNT + ":" + recipientId);
    }

    private NotificationChannelConfig defaultConfig(NotificationType type) {
        return NotificationChannelConfig.builder()
                .notificationType(type).emailEnabled(true).inAppEnabled(true)
                .maxRetries(3).retryDelaySeconds(60).build();
    }
}
