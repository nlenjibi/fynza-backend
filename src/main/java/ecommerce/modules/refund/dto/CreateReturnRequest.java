package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Reason is required")
    private ReturnReason reason;

    private String customerNote;

    @NotEmpty(message = "At least one item must be included in the return")
    @Valid
    private List<ReturnItemRequest> items;
}
