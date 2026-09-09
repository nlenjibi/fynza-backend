package ecommerce.modules.customer.service;

import ecommerce.modules.customer.dto.request.CustomerUpdateRequest;
import ecommerce.modules.customer.dto.response.CustomerDetailResponse;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.dto.response.CustomerStatusHistoryResponse;
import ecommerce.modules.customer.dto.response.CustomerSummaryResponse;
import ecommerce.modules.customer.dto.request.CustomerSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    CustomerResponse provision(UUID userId, String email);

    CustomerDetailResponse getMyCustomer(UUID userId);

    CustomerDetailResponse getCustomerByPublicId(UUID publicId);

    CustomerResponse updateMyCustomer(UUID userId, CustomerUpdateRequest request);

    Page<CustomerSummaryResponse> searchCustomers(CustomerSearchRequest params, Pageable pageable);

    List<CustomerStatusHistoryResponse> getStatusHistory(UUID customerPublicId);
}
