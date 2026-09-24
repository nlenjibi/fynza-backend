package ecommerce.modules.review.scheduler;

import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewModerationRetryJob {

    private final ReviewRepository reviewRepository;
    private static final int MAX_PENDING_HOURS = 48;

    // Runs every hour — flags reviews stuck in PENDING_MODERATION for over 48h
    @Scheduled(fixedDelay = 3_600_000)
    public void flagStalePendingReviews() {
        log.debug("[ReviewModerationRetryJob] Checking for stale pending reviews");
        Instant threshold = Instant.now().minus(MAX_PENDING_HOURS, ChronoUnit.HOURS);
        // Find reviews pending moderation for too long and log them for manual intervention
        // In a production system this would trigger escalation events
        long count = reviewRepository.countByStatusAndCreatedAtBefore(ReviewStatus.PENDING_MODERATION, threshold);
        if (count > 0) {
            log.warn("[ReviewModerationRetryJob] {} review(s) have been PENDING_MODERATION for over {}h — manual review may be needed",
                    count, MAX_PENDING_HOURS);
        }
    }
}
