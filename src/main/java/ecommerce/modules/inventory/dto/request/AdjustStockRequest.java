package ecommerce.modules.inventory.dto.request;

import ecommerce.modules.inventory.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdjustStockRequest {

    @NotNull
    private Integer delta;

    @NotNull
    private MovementType movementType;

    private String reason;
}
