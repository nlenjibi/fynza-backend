package ecommerce.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerInfo {
    private UUID id;
    private String storeName;
    private BigDecimal rating;
}
