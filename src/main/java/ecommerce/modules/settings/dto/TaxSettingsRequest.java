package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSettingsRequest {

    @DecimalMin(value = "0.0", message = "Tax rate must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Tax rate must be between 0 and 100")
    private BigDecimal taxRate;

    @Size(max = 50, message = "Tax number must not exceed 50 characters")
    private String taxNumber;
}
