package ecommerce.modules.authz.dto;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String  displayName;
    private String  description;
    private Boolean active;
}
