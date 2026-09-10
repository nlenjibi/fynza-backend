package ecommerce.modules.store.dto.response;

import ecommerce.modules.store.enums.StoreStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class StoreStatusHistoryResponse {

    UUID id;
    StoreStatus previousStatus;
    StoreStatus newStatus;
    String reason;
    Instant expiresAt;
    Instant createdAt;
}
