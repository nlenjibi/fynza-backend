package ecommerce.modules.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSettingsResponse {
    private UUID id;
    private String smtpHost;
    private String smtpPort;
    private String smtpEmail;
    private LocalDateTime updatedAt;
}
