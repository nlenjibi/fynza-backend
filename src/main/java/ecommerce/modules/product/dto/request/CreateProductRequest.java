package ecommerce.modules.product.dto.request;

import ecommerce.common.enums.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateProductRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    private String brand;

    private String sku;

    private ProductType productType;

    private UUID primaryCategoryId;
}
