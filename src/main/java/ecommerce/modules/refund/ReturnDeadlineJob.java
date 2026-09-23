package ecommerce.modules.refund;

import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnDeadlineJob {

    private static final List<ReturnStatus> EXPIRABLE_STATUSES = List.of(
            ReturnStatus.REQUESTED, ReturnStatus.APPROVED, ReturnStatus.RETURN_SHIPPING);

    private final ReturnRepository returnRepository;
    private final ReturnAuditRecorder auditRecorder;

    @Scheduled(cron = "${returns.deadline.cron:0 0 * * * *}")
    @Transactional
    public void expireOverdueReturns() {
        Instant now = Instant.now();
        List<Return> overdue = returnRepository.findByStatusInAndReturnDeadlineBefore(
                EXPIRABLE_STATUSES, now);

        if (overdue.isEmpty()) {
            return;
        }
        log.info("Expiring {} overdue return(s)", overdue.size());

        for (Return ret : overdue) {
            ReturnStatus previous = ret.getStatus();
            ret.setStatus(ReturnStatus.EXPIRED);
            returnRepository.save(ret);
            auditRecorder.record(ret.getPublicId(), ReturnAuditAction.EXPIRED,
                    previous, ReturnStatus.EXPIRED, null,
                    "Auto-expired: deadline was " + ret.getReturnDeadline());
        }
    }
}
