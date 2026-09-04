package ecommerce.modules.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String q;

    private UUID categoryId;

    private UUID brandId;

    @Positive
    private BigDecimal minPrice;

    @Positive
    private BigDecimal maxPrice;

    @Min(0)
    @Max(5)
    private BigDecimal minRating;

    @Min(0)
    @Max(5)
    private BigDecimal maxRating;

    private Boolean inStock;

    private Boolean expressDelivery;

    @Min(0)
    @Max(100)
    private BigDecimal discountMin;

    @Min(0)
    @Max(100)
    private BigDecimal discountMax;

    @Builder.Default
    private String sortBy = "popularity";

    @Builder.Default
    @Min(0)
    private int page = 0;

    @Builder.Default
    @Positive
    @Max(100)
    private int limit = 20;
}
