package ecommerce.modules.customer.dto.request;

import ecommerce.modules.customer.enums.CustomerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerSearchRequest {

    private String query;
    private CustomerStatus status;
    private String customerNumber;
}
