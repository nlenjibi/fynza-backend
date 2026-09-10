package ecommerce.modules.store.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class StoreSettingResponse {

    UUID id;
    String currency;
    String timezone;
    String language;
    Boolean orderNotifications;
    Boolean customerNotifications;
    Instant updatedAt;
}
