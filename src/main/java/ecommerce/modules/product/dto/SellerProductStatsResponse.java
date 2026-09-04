package ecommerce.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductStatsResponse {
    private long totalProducts;
    private long activeProducts;
    private long draftProducts;
    private long outOfStockProducts;
    private long lowStockProducts;
}
