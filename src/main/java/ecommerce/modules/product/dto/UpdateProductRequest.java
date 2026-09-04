package ecommerce.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRequest {
    private String name;
    private String description;
    private String brand;
    private String sku;
    private UUID categoryId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private List<String> images;
    private Map<String, String> specifications;
}
