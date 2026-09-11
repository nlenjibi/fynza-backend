package ecommerce.modules.customer.dto.response;

import ecommerce.modules.customer.enums.CustomerAddressType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CustomerAddressResponse {

    private UUID id;
    private String recipientName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String country;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private CustomerAddressType addressType;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
