package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySettingsRequest {
    private Boolean twoFactorEnabled;

    @Min(value = 1, message = "Session timeout must be at least 1 minute")
    private Integer sessionTimeoutMinutes;

    private Boolean loginNotifications;
}
