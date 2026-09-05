package ecommerce.graphql.input;

import lombok.Data;

@Data
public class ChangePasswordInput {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
