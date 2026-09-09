package ecommerce.modules.customer.dto.response;

import ecommerce.modules.customer.enums.CustomerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CustomerStatusHistoryResponse {

    private UUID id;
    private CustomerStatus previousStatus;
    private CustomerStatus newStatus;
    private String reason;
    private UUID changedBy;
    private Instant createdAt;
    private Instant expiresAt;
}
