package ecommerce.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVariantRequest {
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer stock;
}
