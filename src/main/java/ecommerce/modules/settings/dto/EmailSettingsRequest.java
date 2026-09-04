package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSettingsRequest {

    @Size(max = 255, message = "SMTP host must not exceed 255 characters")
    private String smtpHost;

    @Size(max = 10, message = "SMTP port must not exceed 10 characters")
    private String smtpPort;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "SMTP email must not exceed 255 characters")
    private String smtpEmail;

    @Size(max = 500, message = "SMTP password must not exceed 500 characters")
    private String smtpPassword;
}
