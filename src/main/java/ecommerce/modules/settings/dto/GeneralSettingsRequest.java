package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating general site settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralSettingsRequest {

    @NotBlank(message = "Site name is required")
    @Size(max = 255, message = "Site name must not exceed 255 characters")
    private String siteName;

    @NotBlank(message = "Site email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String siteEmail;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String sitePhone;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 10, message = "Currency code must be between 3 and 10 characters")
    private String currency;

    @NotBlank(message = "Timezone is required")
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;
}
