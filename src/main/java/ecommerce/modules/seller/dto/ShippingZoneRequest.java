package ecommerce.modules.seller.dto;

import ecommerce.modules.seller.entity.ShippingZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingZoneRequest {
    @NotBlank(message = "Zone name is required")
    private String zoneName;
    
    private String zoneDescription;
    
    @NotBlank(message = "Region is required")
    private String region;
    
    @NotNull(message = "Delivery method is required")
    private ShippingZone.DeliveryMethod deliveryMethod;
    
    @NotNull(message = "Shipping cost is required")
    @Positive(message = "Shipping cost must be positive")
    private BigDecimal shippingCost;
    
    @PositiveOrZero(message = "Free shipping minimum must be zero or positive")
    private BigDecimal freeShippingMin;
    
    private String estimatedDays;
}
