package ecommerce.modules.audit.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Filter criteria for audit log queries. All fields are optional — omitting a field
 * means that dimension is not filtered.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditLogFilter {
    private UUID actorPublicId;
    private String entityType;
    private UUID entityPublicId;
    private List<String> actions;
    private String status;
    private Instant from;
    private Instant to;
}
