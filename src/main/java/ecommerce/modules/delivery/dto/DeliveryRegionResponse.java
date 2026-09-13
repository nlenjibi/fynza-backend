package ecommerce.modules.delivery.dto;

import ecommerce.modules.delivery.entity.DeliveryRegion;
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
public class DeliveryRegionResponse {
    private UUID id;
    private String name;
    private String code;
    private String country;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeliveryRegionResponse from(DeliveryRegion region) {
        return DeliveryRegionResponse.builder()
                .id(region.getPublicId())
                .name(region.getName())
                .code(region.getCode())
                .country(region.getCountry())
                .isActive(region.getIsActive())
                .createdAt(region.getCreatedAt() != null ? region.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .updatedAt(region.getUpdatedAt() != null ? region.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}
