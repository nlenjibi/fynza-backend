package ecommerce.modules.customer.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerSearchRequest;
import ecommerce.modules.customer.dto.request.CustomerUpdateRequest;
import ecommerce.modules.customer.dto.response.*;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerAddress;
import ecommerce.modules.customer.entity.CustomerPreference;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerAlreadyExistsException;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.policy.CustomerOwnershipPolicy;
import ecommerce.modules.customer.repository.CustomerAddressRepository;
import ecommerce.modules.customer.repository.CustomerPreferenceRepository;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import ecommerce.modules.customer.service.CustomerNumberService;
import ecommerce.modules.customer.service.CustomerService;
import ecommerce.modules.customer.spec.CustomerSpec;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository             customerRepository;
    private final CustomerPreferenceRepository   preferenceRepository;
    private final CustomerAddressRepository      addressRepository;
    private final CustomerStatusHistoryRepository historyRepository;
    private final UserRepository                 userRepository;
    private final CustomerNumberService          customerNumberService;
    private final CustomerMapper                 mapper;
    private final AuditLogService                auditLogService;
    private final CustomerOwnershipPolicy        ownershipPolicy;

    @Override
    @Transactional
    public CustomerResponse provision(UUID userId, String email) {
        if (customerRepository.existsByUserId(userId)) {
            throw new CustomerAlreadyExistsException(userId);
        }

        String customerNumber = customerNumberService.generate();

        Customer customer = Customer.builder()
                .userId(userId)
                .customerNumber(customerNumber)
                .status(CustomerStatus.ACTIVE)
                .build();
        customer = customerRepository.save(customer);

        CustomerPreference preference = CustomerPreference.builder()
                .customer(customer)
                .build();
        preferenceRepository.save(preference);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CUSTOMER_PROVISIONED)
                .entityType("CUSTOMER")
                .entityPublicId(customer.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Customer provisioned: customerNumber={}, userId={}", customerNumber, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomerNotFoundException("User not found during provisioning: " + userId));
        return mapper.toResponse(customer, user);
    }

    @Override
    public CustomerDetailResponse getMyCustomer(UUID userId) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        return buildDetailResponse(customer);
    }

    @Override
    public CustomerDetailResponse getCustomerByPublicId(UUID publicId) {
        Customer customer = customerRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomerNotFoundException(publicId));
        return buildDetailResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateMyCustomer(UUID userId, CustomerUpdateRequest request) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        User user = resolveUser(customer.getUserId());

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());
        userRepository.save(user);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CUSTOMER_UPDATED)
                .actorPublicId(userId)
                .entityType("CUSTOMER")
                .entityPublicId(customer.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toResponse(customer, user);
    }

    @Override
    public Page<CustomerSummaryResponse> searchCustomers(CustomerSearchRequest params, Pageable pageable) {
        Specification<Customer> spec = CustomerSpec.fromRequest(params);
        return customerRepository.findAll(spec, pageable)
                .map(c -> mapper.toSummary(c, resolveUser(c.getUserId())));
    }

    @Override
    public List<CustomerStatusHistoryResponse> getStatusHistory(UUID customerPublicId) {
        Customer customer = customerRepository.findByPublicId(customerPublicId)
                .orElseThrow(() -> new CustomerNotFoundException(customerPublicId));
        return historyRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(mapper::toStatusHistoryResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CustomerDetailResponse buildDetailResponse(Customer customer) {
        User user = resolveUser(customer.getUserId());
        CustomerPreference pref = preferenceRepository.findByCustomer_Id(customer.getId()).orElse(null);
        List<CustomerAddress> addresses = addressRepository.findByCustomer_IdAndIsActiveTrue(customer.getId());
        return mapper.toDetailResponse(customer, user, pref, addresses);
    }

    private User resolveUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomerNotFoundException("User not found: " + userId));
    }
}
