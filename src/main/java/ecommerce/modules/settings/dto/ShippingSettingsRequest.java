package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating shipping settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingSettingsRequest {

    @DecimalMin(value = "0.0", message = "Shipping cost must be positive")
    private BigDecimal shippingCost;

    @DecimalMin(value = "0.0", message = "Free shipping threshold must be positive")
    private BigDecimal freeShippingThreshold;

    @Size(max = 20, message = "Estimated delivery days must not exceed 20 characters")
    private String estimatedDeliveryDays;
}
