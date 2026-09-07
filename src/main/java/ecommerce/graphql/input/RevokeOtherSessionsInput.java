package ecommerce.graphql.input;

import lombok.Data;

@Data
public class RevokeOtherSessionsInput {
    private String refreshToken;
}
