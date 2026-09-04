package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchInput {

    private String q;

    private String categoryId;

    private String categoryIds;

    private String categoryName;

    private String categorySlug;

    private String brandId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Float minRating;

    private Float maxRating;

    private Boolean inStock;

    private Boolean expressDelivery;

    private Float discountMin;

    private Float discountMax;

    @Builder.Default
    private String sortBy = "popularity";

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer limit = 20;
}
