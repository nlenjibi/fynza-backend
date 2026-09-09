package ecommerce.modules.authz.scheduler;

import ecommerce.common.util.TokenValidationService;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.authz.entity.UserRoleEntity;
import ecommerce.modules.authz.repository.UserRoleEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleExpiryScheduler {

    private final UserRoleEntityRepository userRoleRepository;
    private final AuditLogService          auditLogService;
    private final TokenValidationService   tokenValidationService;

    @Scheduled(cron = "${authz.role-expiry.cron:0 * * * * *}")
    @Transactional
    public void expireTemporaryRoles() {
        List<UserRoleEntity> expired = userRoleRepository.findExpiredActiveAssignments(Instant.now());
        if (expired.isEmpty()) return;

        log.info("Expiring {} temporary role assignment(s)", expired.size());
        for (UserRoleEntity ur : expired) {
            ur.setActive(false);
            userRoleRepository.save(ur);
            tokenValidationService.evictPrincipal(ur.getUserId());
            auditLogService.logImmediately(AuditLogEntry.builder()
                    .action(AuditAction.TEMPORARY_ACCESS_EXPIRED)
                    .entityType("USER")
                    .entityPublicId(ur.getUserId())
                    .reason("Temporary role expired: " + ur.getRole().getCode())
                    .status(AuditLogEntry.STATUS_SUCCESS)
                    .build());
        }
    }
}
