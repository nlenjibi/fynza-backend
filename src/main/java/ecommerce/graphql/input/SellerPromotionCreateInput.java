package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerPromotionCreateInput {
    private String name;
    private String promotionType;
    private BigDecimal discountValue;
    private BigDecimal minPurchase;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
}
