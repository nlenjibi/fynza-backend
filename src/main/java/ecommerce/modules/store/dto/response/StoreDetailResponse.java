package ecommerce.modules.store.dto.response;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class StoreDetailResponse {

    UUID id;
    String storeName;
    String slug;
    String description;
    String logoMediaId;
    String bannerMediaId;
    StoreStatus status;
    StoreVisibility visibility;
    String businessEmail;
    String businessPhone;
    String website;
    Instant createdAt;
    Instant updatedAt;
    StoreSettingResponse settings;
    List<StorePolicyResponse> policies;
}
