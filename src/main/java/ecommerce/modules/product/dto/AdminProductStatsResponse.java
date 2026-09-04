package ecommerce.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductStatsResponse {
    private long totalProducts;
    private long activeProducts;
    private long pendingProducts;
    private long outOfStockProducts;
    private long lowStockProducts;
}
