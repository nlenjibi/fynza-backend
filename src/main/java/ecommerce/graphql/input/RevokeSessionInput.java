package ecommerce.graphql.input;

import lombok.Data;

import java.util.UUID;

@Data
public class RevokeSessionInput {
    private UUID sessionId;
}
