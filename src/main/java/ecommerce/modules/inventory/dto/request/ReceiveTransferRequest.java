package ecommerce.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReceiveTransferRequest {

    @NotEmpty
    @Valid
    private List<ReceivedItemRequest> items;

    @Data
    public static class ReceivedItemRequest {
        @NotNull
        private Long transferItemId;

        @NotNull
        @Min(0)
        private Integer receivedQuantity;
    }
}
