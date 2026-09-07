package ecommerce.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentAnalyticsDto {

    private String filterPeriod;
    private String startDate;
    private String endDate;
    private Long totalProducts;
    private Double productsChange;
    private Long totalCategories;
    private Double categoriesChange;
    private Long totalOrders;
    private Double ordersChange;
    private Long activeSellers;
    private Double sellersChange;
    private Long totalCustomers;
    private Double customersChange;
    private Long articlesPublished;
    private Double articlesChange;
    private List<MonthlyContentMetric> monthlyTrends;
    private List<CategoryEngagement> categoryEngagement;
    private List<TopContent> topContent;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyContentMetric {
        private String month;
        private Long productsCreated;
        private Long categoriesCreated;
        private Long ordersPlaced;
        private Long newCustomers;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryEngagement {
        private String categoryName;
        private Long productCount;
        private Double avgPrice;
        private Long orderCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopContent {
        private String title;
        private String type;
        private Long views;
        private Long clicks;
        private Double ctr;
        private String trend;
    }
}
