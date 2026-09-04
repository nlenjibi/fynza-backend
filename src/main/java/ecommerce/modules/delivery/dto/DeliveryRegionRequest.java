package ecommerce.modules.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRegionRequest {

    @NotBlank(message = "Region name is required")
    @Size(min = 2, max = 100, message = "Region name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Region code is required")
    @Size(min = 2, max = 10, message = "Region code must be between 2 and 10 characters")
    private String code;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
}
