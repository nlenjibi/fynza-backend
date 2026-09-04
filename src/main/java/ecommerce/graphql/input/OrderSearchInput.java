package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchInput {
    private String query;
    private String status;
    private String paymentStatus;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
