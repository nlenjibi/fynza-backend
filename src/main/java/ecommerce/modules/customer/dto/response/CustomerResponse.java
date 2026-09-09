package ecommerce.modules.customer.dto.response;

import ecommerce.modules.customer.enums.CustomerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CustomerResponse {

    private UUID id;
    private String customerNumber;
    private CustomerStatus status;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;
}
