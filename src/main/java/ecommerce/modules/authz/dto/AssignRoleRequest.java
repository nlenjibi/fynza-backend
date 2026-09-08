package ecommerce.modules.authz.dto;

import ecommerce.common.enums.ScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AssignRoleRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String roleCode;

    @NotNull
    private ScopeType scopeType;

    private UUID    scopeId;
    private Instant expiresAt;
}
