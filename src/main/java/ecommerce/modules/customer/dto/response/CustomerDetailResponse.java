package ecommerce.modules.customer.dto.response;

import ecommerce.modules.customer.enums.CustomerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CustomerDetailResponse {

    private UUID id;
    private String customerNumber;
    private CustomerStatus status;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;
    private CustomerPreferenceResponse preferences;
    private List<CustomerAddressResponse> addresses;
}
