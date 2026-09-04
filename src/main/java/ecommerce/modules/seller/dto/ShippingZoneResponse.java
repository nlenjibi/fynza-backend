package ecommerce.modules.seller.dto;

import ecommerce.modules.seller.entity.ShippingZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingZoneResponse {
    private UUID id;
    private String zoneName;
    private String zoneDescription;
    private String region;
    private ShippingZone.DeliveryMethod deliveryMethod;
    private BigDecimal shippingCost;
    private BigDecimal freeShippingMin;
    private String estimatedDays;
    private Boolean isActive;
    private LocalDateTime updatedAt;
}
