package ecommerce.modules.audit.service;

import ecommerce.modules.audit.dto.AuditLogEntry;

/**
 * Two persistence strategies:
 *
 * <ul>
 *   <li>{@link #log} — event-driven; the audit record commits after the caller's
 *       transaction succeeds. Use for normal business operations.</li>
 *   <li>{@link #logImmediately} — direct write in a new transaction; survives
 *       a caller rollback. Use to record failed or security-sensitive operations
 *       (e.g. FAILED_LOGIN, FAILED_ACCESS).</li>
 * </ul>
 */
public interface AuditLogService {

    void log(AuditLogEntry entry);

    void logImmediately(AuditLogEntry entry);
}
