package ecommerce.modules.delivery.dto;

import ecommerce.common.enums.DeliveryMethod;
import ecommerce.modules.delivery.entity.DeliveryFee;
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
public class DeliveryFeeResponse {
    private UUID id;
    private String townName;
    private DeliveryMethod deliveryMethod;
    private BigDecimal baseFee;
    private BigDecimal perKmFee;
    private Integer estimatedDays;
    private UUID regionId;
    private String regionName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeliveryFeeResponse from(DeliveryFee fee) {
        return DeliveryFeeResponse.builder()
                .id(fee.getId())
                .townName(fee.getTownName())
                .deliveryMethod(fee.getDeliveryMethod())
                .baseFee(fee.getBaseFee())
                .perKmFee(fee.getPerKmFee())
                .estimatedDays(fee.getEstimatedDays())
                .regionId(fee.getRegion() != null ? fee.getRegion().getId() : null)
                .regionName(fee.getRegion() != null ? fee.getRegion().getName() : null)
                .isActive(fee.getIsActive())
                .createdAt(fee.getCreatedAt())
                .updatedAt(fee.getUpdatedAt())
                .build();
    }
}
