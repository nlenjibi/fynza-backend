package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.RefundMethod;
import ecommerce.modules.refund.enums.ReturnPolicyScope;
import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnShippingResponsibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPolicyResponse {

    private UUID publicId;
    private String name;
    private ReturnPolicyScope scope;
    private UUID storeId;
    private UUID productId;
    private UUID categoryId;
    private Integer returnWindowDays;
    private Boolean isReturnable;
    private Boolean conditionRequired;
    private List<ReturnReason> eligibleReasons;
    private ReturnShippingResponsibility returnShippingResponsibility;
    private BigDecimal restockingFeePercent;
    private RefundMethod refundMethod;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
