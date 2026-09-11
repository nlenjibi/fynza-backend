package ecommerce.graphql.input;

import lombok.Data;

@Data
public class MfaDisableInput {
    private String totpCode;
}
