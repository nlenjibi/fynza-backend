package ecommerce.modules.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for payment settings.
 * Sensitive keys are masked for security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSettingsResponse {

    private UUID id;
    private String paystackPublicKey; // Masked
    private Boolean enableCashOnDelivery;
    private Boolean enableMobileMoney;
    private LocalDateTime updatedAt;
}
