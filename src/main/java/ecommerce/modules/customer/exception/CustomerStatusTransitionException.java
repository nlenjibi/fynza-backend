package ecommerce.modules.customer.exception;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.customer.enums.CustomerStatus;

public class CustomerStatusTransitionException extends BadRequestException {

    public CustomerStatusTransitionException(CustomerStatus from, CustomerStatus to) {
        super("Invalid customer status transition: " + from + " → " + to);
    }
}
