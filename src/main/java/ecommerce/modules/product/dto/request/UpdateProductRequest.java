package ecommerce.modules.product.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    private String brand;

    private String sku;
}
