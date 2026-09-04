package ecommerce.modules.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerShippingSettingsResponse {
    private UUID id;
    private String returnPolicy;
    private List<ShippingZoneResponse> shippingZones;
    private LocalDateTime updatedAt;
}
