package ecommerce.modules.user.dto;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import lombok.Data;

@Data
public class AdminUserSearchParams {
    private String query;
    private UserStatus status;
    private Role role;
    private Boolean emailVerified;
}
