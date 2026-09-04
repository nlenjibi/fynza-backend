package ecommerce.modules.delivery.dto;

import ecommerce.common.enums.DeliveryMethod;
import ecommerce.modules.delivery.entity.DeliveryFee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFeeRequest {

    @NotBlank(message = "Town name is required")
    private String townName;

    @NotNull(message = "Delivery method is required")
    private DeliveryMethod deliveryMethod;

    @NotNull(message = "Base fee is required")
    @Positive(message = "Base fee must be positive")
    private BigDecimal baseFee;

    @NotNull(message = "Per KM fee is required")
    @Positive(message = "Per KM fee must be positive")
    private BigDecimal perKmFee;

    private BigDecimal weightBasedFee;
    private BigDecimal freeShippingThreshold;

    @NotNull(message = "Estimated days is required")
    @Positive(message = "Estimated days must be positive")
    private Integer estimatedDays;

    private UUID regionId;
}
