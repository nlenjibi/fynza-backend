package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateShippingZoneRequest {

    @NotBlank(message = "Zone name is required")
    @Size(max = 100)
    private String name;

    private String description;

    @NotEmpty(message = "At least one region is required")
    private List<String> regions;
}
