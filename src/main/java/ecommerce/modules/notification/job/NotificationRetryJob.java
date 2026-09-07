package ecommerce.modules.notification.job;

import ecommerce.modules.notification.dto.EmailRequest;
import ecommerce.modules.notification.entity.NotificationDispatch;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationStatus;
import ecommerce.modules.notification.exceptions.EmailDispatchException;
import ecommerce.modules.notification.exceptions.SlackDispatchException;
import ecommerce.modules.notification.provider.EmailProvider;
import ecommerce.modules.notification.provider.SlackClient;
import ecommerce.modules.notification.repository.NotificationChannelConfigRepository;
import ecommerce.modules.notification.repository.NotificationDispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final NotificationDispatchRepository      dispatchRepo;
    private final NotificationChannelConfigRepository channelConfigRepo;
    private final EmailProvider                       emailProvider;
    private final SlackClient                         slackClient;

    @Value("${fynza.notification.email.from}")
    private String fromAddress;

    @Scheduled(cron = "${fynza.notification.retry-job.cron:0 */2 * * * *}")
    @Transactional
    public void retryPendingDispatches() {
        List<NotificationDispatch> due =
                dispatchRepo.findDueForRetry(NotificationStatus.PENDING, Instant.now());

        if (due.isEmpty()) return;
        log.info("[NotificationRetry] Processing {} pending dispatch(es)", due.size());
        due.forEach(this::retryOne);
    }

    private void retryOne(NotificationDispatch dispatch) {
        var config    = channelConfigRepo.findByNotificationType(dispatch.getNotificationType());
        int maxRetries  = config.map(c -> c.getMaxRetries()).orElse(3);
        int retryDelay  = config.map(c -> c.getRetryDelaySeconds()).orElse(60);

        if (dispatch.getAttemptCount() >= maxRetries) {
            dispatch.setStatus(NotificationStatus.FAILED);
            dispatch.setFailureReason("MAX_RETRIES_EXCEEDED after " + dispatch.getAttemptCount() + " attempt(s)");
            dispatchRepo.save(dispatch);
            log.warn("[NotificationRetry] Giving up id={} type={} attempts={}",
                    dispatch.getId(), dispatch.getNotificationType(), dispatch.getAttemptCount());
            return;
        }

        dispatch.setStatus(NotificationStatus.SENDING);
        dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
        dispatch.setScheduledAt(null);
        dispatchRepo.save(dispatch);

        if (dispatch.getChannel() == NotificationChannel.SLACK) {
            retrySlack(dispatch, retryDelay);
        } else {
            retryEmail(dispatch, retryDelay);
        }
    }

    private void retryEmail(NotificationDispatch dispatch, int retryDelay) {
        try {
            EmailRequest req = EmailRequest.builder()
                    .from(fromAddress)
                    .to(List.of(dispatch.getRecipientEmail()))
                    .subject(dispatch.getSubject())
                    .textBody(dispatch.getTextBody() != null ? dispatch.getTextBody() : dispatch.getSubject())
                    .build();

            String messageId = emailProvider.send(req);
            dispatch.setStatus(NotificationStatus.SENT);
            dispatch.setProviderMessageId(messageId);
            dispatch.setSentAt(Instant.now());
            dispatchRepo.save(dispatch);
            log.info("[NotificationRetry] Retry succeeded id={}", dispatch.getId());

        } catch (EmailDispatchException ex) {
            long nextDelay = retryDelay * (long) Math.pow(2, (double) dispatch.getAttemptCount() - 1);
            dispatch.setStatus(NotificationStatus.PENDING);
            dispatch.setFailureReason(ex.getMessage());
            dispatch.setScheduledAt(Instant.now().plusSeconds(nextDelay));
            dispatchRepo.save(dispatch);
            log.warn("[NotificationRetry] Retry failed id={} nextIn={}s", dispatch.getId(), nextDelay);

        } catch (Exception ex) {
            dispatch.setStatus(NotificationStatus.FAILED);
            dispatch.setFailureReason("Unexpected: " + ex.getMessage());
            dispatchRepo.save(dispatch);
            log.error("[NotificationRetry] Unexpected error id={}", dispatch.getId(), ex);
        }
    }

    private void retrySlack(NotificationDispatch dispatch, int retryDelay) {
        try {
            slackClient.send(dispatch.getSlackChannelId(), dispatch.getTextBody());
            dispatch.setStatus(NotificationStatus.SENT);
            dispatch.setSentAt(Instant.now());
            dispatchRepo.save(dispatch);
            log.info("[NotificationRetry] SLACK retry succeeded id={}", dispatch.getId());

        } catch (SlackDispatchException ex) {
            if (!ex.isRetryable()) {
                dispatch.setStatus(NotificationStatus.FAILED);
                dispatch.setFailureReason(ex.getMessage());
                dispatchRepo.save(dispatch);
                log.warn("[NotificationRetry] SLACK retry permanently failed id={}", dispatch.getId());
                return;
            }
            long nextDelay = retryDelay * (long) Math.pow(2, (double) dispatch.getAttemptCount() - 1);
            dispatch.setStatus(NotificationStatus.PENDING);
            dispatch.setFailureReason(ex.getMessage());
            dispatch.setScheduledAt(Instant.now().plusSeconds(nextDelay));
            dispatchRepo.save(dispatch);
            log.warn("[NotificationRetry] SLACK retry failed id={} nextIn={}s", dispatch.getId(), nextDelay);

        } catch (Exception ex) {
            dispatch.setStatus(NotificationStatus.FAILED);
            dispatch.setFailureReason("Unexpected: " + ex.getMessage());
            dispatchRepo.save(dispatch);
            log.error("[NotificationRetry] Unexpected error id={}", dispatch.getId(), ex);
        }
    }
}
