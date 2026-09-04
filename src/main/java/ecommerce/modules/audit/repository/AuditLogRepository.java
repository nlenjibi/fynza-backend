package ecommerce.modules.audit.repository;

import ecommerce.modules.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository for the append-only audit_log table.
 * JpaSpecificationExecutor supports dynamic filter-based queries in the query service.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {}
