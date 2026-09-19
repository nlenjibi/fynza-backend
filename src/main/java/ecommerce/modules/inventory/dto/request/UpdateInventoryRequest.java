package ecommerce.modules.inventory.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateInventoryRequest {

    @Min(0)
    private Integer lowStockThreshold;

    private Boolean allowBackorder;

    private Boolean isActive;
}
