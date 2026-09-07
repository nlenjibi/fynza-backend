package ecommerce.modules.audit.service;

import ecommerce.modules.audit.dto.AuditLogDetailResponse;
import ecommerce.modules.audit.dto.AuditLogFilter;
import ecommerce.modules.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Query contract for the audit log — all endpoints require ADMIN role;
 * there is no building/org scoping in Fynza (unlike OMS).
 */
public interface AuditLogQueryService {

    Page<AuditLogResponse> query(AuditLogFilter filter, Pageable pageable);

    AuditLogDetailResponse getById(UUID logId);
}
