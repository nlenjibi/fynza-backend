package ecommerce.modules.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ReserveStockRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    private UUID orderId;

    private Instant expiresAt;
}
