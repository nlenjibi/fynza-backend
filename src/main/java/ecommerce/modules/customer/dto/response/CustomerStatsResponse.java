package ecommerce.modules.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerStatsResponse {
    private Long totalCustomers;
    private Long activeCustomers;
    private Long newCustomersThisMonth;
}
