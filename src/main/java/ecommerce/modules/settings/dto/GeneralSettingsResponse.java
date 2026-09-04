package ecommerce.modules.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for general site settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralSettingsResponse {

    private UUID id;
    private String siteName;
    private String siteEmail;
    private String sitePhone;
    private String currency;
    private String timezone;
    private LocalDateTime updatedAt;
}
