package ecommerce.modules.customer.dto.request;

import ecommerce.modules.customer.enums.CustomerAddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomerAddressRequest {

    @NotBlank
    @Size(max = 150)
    private String recipientName;

    @Size(max = 30)
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String region;

    @NotBlank
    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String postalCode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private CustomerAddressType addressType;
    private Boolean isDefault;
}
