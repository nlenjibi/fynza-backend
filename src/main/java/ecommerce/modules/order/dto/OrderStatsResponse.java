package ecommerce.modules.order.dto;

import ecommerce.modules.order.entity.OrderStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatsResponse {
    private OrderStats stats;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
