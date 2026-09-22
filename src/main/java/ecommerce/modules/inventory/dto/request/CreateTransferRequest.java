package ecommerce.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateTransferRequest {

    @NotNull
    private Long sourceLocationId;

    @NotNull
    private Long destinationLocationId;

    @NotEmpty
    @Valid
    private List<TransferItemRequest> items;

    @Data
    public static class TransferItemRequest {
        @NotNull
        private UUID inventoryPublicId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
