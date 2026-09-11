package ecommerce.modules.customer.service;

import ecommerce.modules.customer.dto.request.CustomerStatusRequest;
import ecommerce.modules.customer.dto.response.CustomerResponse;

import java.util.UUID;

public interface CustomerStatusService {

    CustomerResponse suspendCustomer(UUID customerPublicId, UUID actorId, CustomerStatusRequest request);

    CustomerResponse activateCustomer(UUID customerPublicId, UUID actorId);

    CustomerResponse blockCustomer(UUID customerPublicId, UUID actorId, CustomerStatusRequest request);
}
