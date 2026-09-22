package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCarrierRequest {

    @NotBlank(message = "Carrier name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Carrier code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric")
    private String code;

    private String logoUrl;
    private String trackingUrlTemplate;
}
