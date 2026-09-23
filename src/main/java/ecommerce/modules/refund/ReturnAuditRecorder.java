package ecommerce.modules.refund;

import ecommerce.modules.refund.entity.ReturnAudit;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.repository.ReturnAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReturnAuditRecorder {

    private final ReturnAuditRepository auditRepository;

    public void record(UUID returnId, ReturnAuditAction action,
                       ReturnStatus previousStatus, ReturnStatus newStatus,
                       UUID performedBy, String reason) {
        auditRepository.save(ReturnAudit.builder()
                .returnId(returnId)
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .performedBy(performedBy)
                .reason(reason)
                .build());
    }

    public void record(UUID returnId, ReturnAuditAction action, UUID performedBy, String reason) {
        record(returnId, action, null, null, performedBy, reason);
    }
}
