package ecommerce.graphql.input;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFilterInput {
    private String search;
    private Role role;
    private UserStatus status;
    private Boolean active;
    private Boolean emailVerified;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String phoneNumber;
    private String name;
}
