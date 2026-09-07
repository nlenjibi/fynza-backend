package ecommerce.modules.audit.service;

import ecommerce.modules.audit.dto.AuditLogFilter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Exports audit log data as CSV using keyset pagination to avoid
 * loading the full result set into memory.
 */
public interface AuditLogExportService {

    StreamingResponseBody exportCsv(AuditLogFilter filter);
}
