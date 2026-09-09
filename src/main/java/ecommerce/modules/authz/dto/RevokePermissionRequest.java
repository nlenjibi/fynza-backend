package ecommerce.modules.authz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RevokePermissionRequest {

    @NotBlank
    private String roleCode;

    @NotBlank
    private String permissionCode;
}
