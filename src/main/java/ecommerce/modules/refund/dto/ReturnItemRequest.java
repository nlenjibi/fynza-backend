package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnItemCondition;
import ecommerce.modules.refund.enums.ReturnReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemRequest {

    @NotNull(message = "Order item ID is required")
    private UUID orderItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Reason is required")
    private ReturnReason reason;

    private ReturnItemCondition condition;
}
