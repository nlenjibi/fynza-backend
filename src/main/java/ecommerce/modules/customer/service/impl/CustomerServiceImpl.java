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
import ecommerce.modules.customer.repository.CustomerDetailViewRepository;
import ecommerce.modules.customer.repository.CustomerPreferenceRepository;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import ecommerce.modules.customer.repository.CustomerStatsViewRepository;
import ecommerce.modules.customer.repository.CustomerSummaryViewRepository;
import ecommerce.modules.customer.service.CustomerNumberService;
import ecommerce.modules.customer.service.CustomerService;
import ecommerce.modules.customer.spec.CustomerSummarySpec;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository              customerRepository;
    private final CustomerPreferenceRepository    preferenceRepository;
    private final CustomerAddressRepository       addressRepository;
    private final CustomerStatusHistoryRepository historyRepository;
    private final CustomerSummaryViewRepository   summaryViewRepository;
    private final CustomerDetailViewRepository    detailViewRepository;
    private final CustomerStatsViewRepository     statsViewRepository;
    private final UserRepository                  userRepository;
    private final CustomerNumberService           customerNumberService;
    private final CustomerMapper                  mapper;
    private final AuditLogService                 auditLogService;
    private final CustomerOwnershipPolicy         ownershipPolicy;

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
        var view = detailViewRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException("No customer record found for current user"));
        List<CustomerAddress> addresses = addressRepository.findByCustomer_IdAndIsActiveTrue(view.getId());
        return mapper.toDetailResponse(view, addresses);
    }

    @Override
    public CustomerDetailResponse getCustomerByPublicId(UUID publicId) {
        var view = detailViewRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomerNotFoundException(publicId));
        List<CustomerAddress> addresses = addressRepository.findByCustomer_IdAndIsActiveTrue(view.getId());
        return mapper.toDetailResponse(view, addresses);
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
        return summaryViewRepository.findAll(CustomerSummarySpec.fromRequest(params), pageable)
                .map(mapper::toSummary);
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

    @Override
    public CustomerStatsResponse getCustomerStats() {
        return statsViewRepository.findById(1)
                .map(mapper::toStats)
                .orElse(CustomerStatsResponse.builder()
                        .totalCustomers(0L)
                        .activeCustomers(0L)
                        .newCustomersThisMonth(0L)
                        .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User resolveUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomerNotFoundException("User not found: " + userId));
    }
}
