package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating payment settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSettingsRequest {

    @Size(max = 500, message = "Paystack public key must not exceed 500 characters")
    private String paystackPublicKey;

    @Size(max = 500, message = "Paystack secret key must not exceed 500 characters")
    private String paystackSecretKey;

    private Boolean enableCashOnDelivery;

    private Boolean enableMobileMoney;
}
