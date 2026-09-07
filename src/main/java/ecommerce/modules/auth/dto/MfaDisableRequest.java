package ecommerce.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MfaDisableRequest {

    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "\\d{6}", message = "TOTP code must be exactly 6 digits")
    private String totpCode;
}
