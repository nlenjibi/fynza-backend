package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStats {
    private Long totalUsers;
    private BigDecimal totalRevenue;
    private Long totalOrderCount;
    private Long totalProducts;
    private Long totalSellers;
    private Long totalCustomers;
}
