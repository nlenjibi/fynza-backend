package ecommerce.graphql.input;

import lombok.Data;

@Data
public class ResetPasswordInput {
    private String token;
    private String newPassword;
    private String confirmPassword;
}
