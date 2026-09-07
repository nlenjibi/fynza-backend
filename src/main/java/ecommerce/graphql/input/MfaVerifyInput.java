package ecommerce.graphql.input;

import lombok.Data;

@Data
public class MfaVerifyInput {
    private String challengeToken;
    private String totpCode;
}
