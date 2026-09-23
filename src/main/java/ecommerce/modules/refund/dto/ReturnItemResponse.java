package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnItemCondition;
import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnResolution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemResponse {

    private UUID publicId;
    private UUID orderItemId;
    private UUID productId;
    private String productName;
    private Integer quantity;
    private ReturnReason reason;
    private ReturnItemCondition condition;
    private BigDecimal unitPrice;
    private Integer approvedQuantity;
    private Integer receivedQuantity;
    private ReturnResolution resolution;
    private Instant createdAt;
}
