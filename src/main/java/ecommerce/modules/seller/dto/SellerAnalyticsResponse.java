package ecommerce.modules.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerAnalyticsResponse {

    private BigDecimal totalSales;
    private BigDecimal averageOrderValue;
    private Long totalOrders;
    private Long totalProductsSold;
    private Double conversionRate;
    private List<DailySales> dailySales;
    private List<TopProduct> topProducts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySales {
        private String date;
        private BigDecimal sales;
        private Long orders;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProduct {
        private String productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }
}
