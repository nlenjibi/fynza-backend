package ecommerce.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterDeviceRequest {

    @NotBlank
    @Pattern(regexp = "IOS|ANDROID|WEB", message = "platform must be IOS, ANDROID, or WEB")
    private String platform;

    @NotBlank
    private String deviceToken;

    private String appVersion;
}
