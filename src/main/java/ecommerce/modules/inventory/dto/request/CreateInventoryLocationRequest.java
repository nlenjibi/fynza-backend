package ecommerce.modules.inventory.dto.request;

import ecommerce.modules.inventory.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateInventoryLocationRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotNull
    private LocationType locationType;

    private Long storeId;
}
