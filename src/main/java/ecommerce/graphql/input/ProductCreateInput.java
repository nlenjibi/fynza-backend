package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateInput {
    private String name;
    private String description;
    private String brand;
    private String sku;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private UUID categoryId;
    private Integer stock;
    private List<String> images;
}
