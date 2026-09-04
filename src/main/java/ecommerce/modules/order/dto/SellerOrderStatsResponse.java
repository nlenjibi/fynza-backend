package ecommerce.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrderStatsResponse {
    private long totalOrders;
    private long pending;
    private long confirmed;
    private long processing;
    private long shipped;
    private long delivered;
    private long cancelled;
    private long refunded;
}
