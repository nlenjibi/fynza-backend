package ecommerce.modules.store.dto.response;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class StoreSummaryResponse {

    UUID id;
    String storeName;
    String slug;
    String logoMediaId;
    StoreStatus status;
    StoreVisibility visibility;
    String sellerDisplayName;
    Long productCount;
    Double avgRating;
    Long reviewCount;
    Long orderCount;
    Instant createdAt;
}
