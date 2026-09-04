package ecommerce.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private UUID id;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer stock;
}
