package ecommerce.modules.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CustomerPreferenceResponse {

    private UUID id;
    private String language;
    private String currency;
    private Boolean marketingOptIn;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
    private Instant updatedAt;
}
