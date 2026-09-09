package ecommerce.modules.authz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GrantPermissionRequest {

    @NotBlank
    private String roleCode;

    @NotBlank
    private String permissionCode;
}
