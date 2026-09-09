package ecommerce.modules.customer.validation;

import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class CustomerStatusTransitionValidator {

    private static final Map<CustomerStatus, Set<CustomerStatus>> ALLOWED = new EnumMap<>(CustomerStatus.class);

    static {
        ALLOWED.put(CustomerStatus.PROSPECT,  EnumSet.of(CustomerStatus.ACTIVE, CustomerStatus.DELETED));
        ALLOWED.put(CustomerStatus.ACTIVE,    EnumSet.of(CustomerStatus.INACTIVE, CustomerStatus.SUSPENDED, CustomerStatus.BLOCKED, CustomerStatus.DELETED));
        ALLOWED.put(CustomerStatus.INACTIVE,  EnumSet.of(CustomerStatus.ACTIVE, CustomerStatus.DELETED));
        ALLOWED.put(CustomerStatus.SUSPENDED, EnumSet.of(CustomerStatus.ACTIVE, CustomerStatus.BLOCKED, CustomerStatus.DELETED));
        ALLOWED.put(CustomerStatus.BLOCKED,   EnumSet.of(CustomerStatus.DELETED));
        ALLOWED.put(CustomerStatus.DELETED,   EnumSet.noneOf(CustomerStatus.class));
    }

    public void validate(CustomerStatus from, CustomerStatus to) {
        Set<CustomerStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(CustomerStatus.class));
        if (!allowed.contains(to)) {
            throw new CustomerStatusTransitionException(from, to);
        }
    }
}
