package ecommerce.graphql.input;

import lombok.Data;

@Data
public class MfaEnableInput {
    private String totpCode;
}
