package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStats {
    private Long totalCustomers;
    private Long activeCustomers;
    private Long newCustomersThisMonth;
}
