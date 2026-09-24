package ecommerce.modules.notification.job;

import ecommerce.modules.notification.repository.NotificationDispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationExpirationJob {

    private final NotificationDispatchRepository dispatchRepo;

    /**
     * Marks PENDING dispatches whose scheduled_at is in the past but whose
     * source notification has expired as FAILED so the retry job ignores them.
     * Runs hourly.
     */
    @Scheduled(cron = "${fynza.notification.expiration-job.cron:0 0 * * * *}")
    @Transactional
    public void cancelExpiredDispatches() {
        int cancelled = dispatchRepo.cancelExpiredPendingDispatches(Instant.now());
        if (cancelled > 0) {
            log.info("[NotificationExpiry] Cancelled {} expired pending dispatch(es)", cancelled);
        }
    }
}
