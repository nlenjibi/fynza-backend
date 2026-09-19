package ecommerce.modules.pricing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceListResponse {

    private UUID publicId;
    private String name;
    private String description;
    private Boolean isDefault;
    private Boolean isActive;
    private Instant createdAt;
}
