package ecommerce.modules.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRoleRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]+$", message = "Role code must be uppercase with underscores")
    private String code;

    private String displayName;
    private String description;
}
