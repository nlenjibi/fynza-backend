package ecommerce.modules.user.dto;

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
public class CustomerDashboardResponse {

    private Long totalOrders;
    private Long wishlistItems;
    private Long savedAddresses;
    private Integer loyaltyPoints;
    private BigDecimal totalSpent;
    private String membershipStatus;
    private List<RecentOrderDto> recentOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderDto {
        private String orderId;
        private String orderNumber;
        private String orderDate;
        private String status;
        private BigDecimal totalAmount;
    }
}
