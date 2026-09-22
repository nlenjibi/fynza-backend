package ecommerce.modules.payout.dto.request;

import ecommerce.modules.payout.enums.PayoutAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddPayoutAccountRequest {

    @NotNull(message = "Account type is required")
    private PayoutAccountType type;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Country is required")
    private String country;

    private String currency = "GHS";

    private Boolean isDefault = false;
}
