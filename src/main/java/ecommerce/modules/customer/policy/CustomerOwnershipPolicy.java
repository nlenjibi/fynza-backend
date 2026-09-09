package ecommerce.modules.customer.policy;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerOwnershipPolicy {

    private final CustomerRepository customerRepository;

    public Customer assertOwns(UUID customerPublicId, UUID actorUserId) {
        Customer customer = customerRepository.findByPublicId(customerPublicId)
                .orElseThrow(() -> new CustomerNotFoundException(customerPublicId));
        if (!customer.getUserId().equals(actorUserId)) {
            throw new ForbiddenException("Access denied: customer does not belong to requesting user");
        }
        return customer;
    }

    public Customer resolveOwn(UUID actorUserId) {
        return customerRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new CustomerNotFoundException("No customer record found for current user"));
    }
}
