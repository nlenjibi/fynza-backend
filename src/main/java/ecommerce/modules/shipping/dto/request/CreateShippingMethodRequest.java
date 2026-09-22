package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateShippingMethodRequest {

    @NotNull(message = "Carrier ID is required")
    private UUID carrierId;

    @NotBlank(message = "Method name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Method code is required")
    @Size(max = 50)
    private String code;

    private String description;

    @Min(1)
    private Integer estimatedDaysMin = 1;

    @Min(1)
    private Integer estimatedDaysMax = 7;
}
