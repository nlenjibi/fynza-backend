package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.RefundMethod;
import ecommerce.modules.refund.enums.ReturnPolicyScope;
import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnShippingResponsibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnPolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String name;

    @NotNull(message = "Scope is required")
    private ReturnPolicyScope scope;

    private UUID storeId;
    private UUID productId;
    private UUID categoryId;

    @NotNull(message = "Return window days is required")
    @Min(value = 1,   message = "Return window must be at least 1 day")
    @Max(value = 365, message = "Return window cannot exceed 365 days")
    private Integer returnWindowDays;

    @Builder.Default
    private Boolean isReturnable = true;

    @Builder.Default
    private Boolean conditionRequired = false;

    private List<ReturnReason> eligibleReasons;

    @Builder.Default
    private ReturnShippingResponsibility returnShippingResponsibility = ReturnShippingResponsibility.CUSTOMER;

    @Min(value = 0, message = "Restocking fee cannot be negative")
    @Max(value = 100, message = "Restocking fee cannot exceed 100%")
    private BigDecimal restockingFeePercent;

    @Builder.Default
    private RefundMethod refundMethod = RefundMethod.ORIGINAL_PAYMENT;
}
