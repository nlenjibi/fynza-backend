package ecommerce.modules.customer.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerStatusRequest;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerStatusHistory;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import ecommerce.modules.customer.service.CustomerStatusService;
import ecommerce.modules.customer.validation.CustomerStatusTransitionValidator;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerStatusServiceImpl implements CustomerStatusService {

    private final CustomerRepository              customerRepository;
    private final CustomerStatusHistoryRepository historyRepository;
    private final UserRepository                  userRepository;
    private final CustomerMapper                  mapper;
    private final AuditLogService                 auditLogService;
    private final CustomerStatusTransitionValidator transitionValidator;

    @Override
    @Transactional
    public CustomerResponse suspendCustomer(UUID customerPublicId, UUID actorId, CustomerStatusRequest request) {
        return applyTransition(customerPublicId, actorId, CustomerStatus.SUSPENDED,
                request.getReason(), request.getExpiresAt() != null ? request.getExpiresAt() : null,
                AuditAction.CUSTOMER_SUSPENDED);
    }

    @Override
    @Transactional
    public CustomerResponse activateCustomer(UUID customerPublicId, UUID actorId) {
        return applyTransition(customerPublicId, actorId, CustomerStatus.ACTIVE,
                "Administrative reactivation", null, AuditAction.CUSTOMER_ACTIVATED);
    }

    @Override
    @Transactional
    public CustomerResponse blockCustomer(UUID customerPublicId, UUID actorId, CustomerStatusRequest request) {
        return applyTransition(customerPublicId, actorId, CustomerStatus.BLOCKED,
                request.getReason(), null, AuditAction.CUSTOMER_BLOCKED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CustomerResponse applyTransition(UUID customerPublicId, UUID actorId,
                                             CustomerStatus newStatus, String reason,
                                             java.time.Instant expiresAt, String auditAction) {
        Customer customer = customerRepository.findByPublicId(customerPublicId)
                .orElseThrow(() -> new CustomerNotFoundException(customerPublicId));

        CustomerStatus previous = customer.getStatus();
        transitionValidator.validate(previous, newStatus);

        customer.setStatus(newStatus);
        customerRepository.save(customer);

        historyRepository.save(CustomerStatusHistory.builder()
                .customerId(customer.getId())
                .previousStatus(previous)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(actorId)
                .expiresAt(expiresAt)
                .build());

        auditLogService.log(AuditLogEntry.builder()
                .action(auditAction)
                .actorPublicId(actorId)
                .entityType("CUSTOMER")
                .entityPublicId(customerPublicId)
                .previousState(Map.of("status", previous.name()))
                .newState(Map.of("status", newStatus.name()))
                .reason(reason)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Customer status changed: publicId={}, {} → {}, actor={}", customerPublicId, previous, newStatus, actorId);

        User user = userRepository.findById(customer.getUserId())
                .orElseThrow(() -> new CustomerNotFoundException("User not found: " + customer.getUserId()));
        return mapper.toResponse(customer, user);
    }
}
