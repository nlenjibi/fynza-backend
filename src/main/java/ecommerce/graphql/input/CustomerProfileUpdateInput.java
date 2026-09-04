package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileUpdateInput {
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
}
