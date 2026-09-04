package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterInput {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
}
