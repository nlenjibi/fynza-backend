package ecommerce.modules.store.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreSettingRequest {

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;

    @Size(max = 50)
    private String timezone;

    @Size(max = 10)
    private String language;

    private Boolean orderNotifications;

    private Boolean customerNotifications;
}
