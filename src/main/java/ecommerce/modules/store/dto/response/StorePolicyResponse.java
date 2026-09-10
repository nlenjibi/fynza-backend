package ecommerce.modules.store.dto.response;

import ecommerce.modules.store.enums.StorePolicyStatus;
import ecommerce.modules.store.enums.StorePolicyType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class StorePolicyResponse {

    UUID id;
    StorePolicyType type;
    String title;
    String content;
    Integer version;
    StorePolicyStatus status;
    Instant effectiveFrom;
    Instant createdAt;
    Instant updatedAt;
}
