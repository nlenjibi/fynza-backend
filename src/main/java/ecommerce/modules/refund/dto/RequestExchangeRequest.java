package ecommerce.modules.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestExchangeRequest {

    @NotBlank(message = "Exchange item description is required")
    private String requestedItemsDescription;

    private String notes;
}
