package ecommerce.modules.customer.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerPreferenceRequest {

    @Size(min = 2, max = 10)
    private String language;

    @Size(min = 3, max = 3)
    private String currency;

    private Boolean marketingOptIn;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
}
