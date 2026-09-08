package ecommerce.modules.authz.dto;

import ecommerce.common.enums.ScopeType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserRoleDto {
    UUID      id;
    String    roleCode;
    String    roleDisplayName;
    ScopeType scopeType;
    UUID      scopeId;
    UUID      assignedBy;
    Instant   assignedAt;
    Instant   expiresAt;
    boolean   active;
}
