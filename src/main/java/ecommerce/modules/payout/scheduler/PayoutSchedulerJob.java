package ecommerce.modules.payout.scheduler;

import ecommerce.modules.payout.entity.Payout;
import ecommerce.modules.payout.enums.PayoutStatus;
import ecommerce.modules.payout.provider.PayoutProperties;
import ecommerce.modules.payout.repository.PayoutRepository;
import ecommerce.modules.payout.service.PayoutAdminService;
import ecommerce.modules.payout.service.PayoutProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutSchedulerJob {

    private final PayoutRepository payoutRepository;
    private final PayoutProcessingService payoutProcessingService;
    private final PayoutAdminService payoutAdminService;
    private final PayoutProperties props;

    @Scheduled(cron = "${fynza.payout.scheduler.process-cron:0 */5 * * * *}")
    public void processApprovedPayouts() {
        List<Payout> approved = payoutRepository.findApprovedPayoutsForProcessing(PageRequest.of(0, 20));

        if (approved.isEmpty()) {
            return;
        }

        log.info("Processing {} approved payout(s)", approved.size());
        for (Payout p : approved) {
            try {
                payoutProcessingService.processPayout(p.getId());
            } catch (Exception e) {
                log.error("Failed to process payout id={}: {}", p.getId(), e.getMessage(), e);
            }
        }
    }

    @Scheduled(cron = "${fynza.payout.scheduler.retry-cron:0 0 * * * *}")
    public void retryFailedPayouts() {
        List<Payout> retryable = payoutRepository.findRetryablePayouts(
                List.of(PayoutStatus.FAILED), props.getMaxRetryCount(), PageRequest.of(0, 10));

        if (retryable.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed payout(s)", retryable.size());
        for (Payout p : retryable) {
            try {
                payoutAdminService.retryPayout(p.getPublicId());
            } catch (Exception e) {
                log.error("Retry failed for payout id={}: {}", p.getId(), e.getMessage(), e);
            }
        }
    }
}
