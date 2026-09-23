package ecommerce.modules.shipping.dto.request;

import ecommerce.modules.shipping.enums.PackageType;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequest {

    @Positive
    private BigDecimal weightKg;

    @Positive
    private BigDecimal lengthCm;

    @Positive
    private BigDecimal widthCm;

    @Positive
    private BigDecimal heightCm;

    private PackageType packageType;

    private String labelReference;
}
