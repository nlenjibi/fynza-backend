package ecommerce.modules.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVariantRequest {

    @NotBlank
    @Size(max = 255)
    private String sku;

    private String barcode;

    @Size(max = 255)
    private String variantName;

    @Size(max = 50)
    private String size;

    @Size(max = 50)
    private String color;
}
