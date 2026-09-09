package ecommerce.modules.customer.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerPreferenceRequest;
import ecommerce.modules.customer.dto.response.CustomerPreferenceResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerPreference;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.policy.CustomerOwnershipPolicy;
import ecommerce.modules.customer.repository.CustomerPreferenceRepository;
import ecommerce.modules.customer.service.CustomerPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerPreferenceServiceImpl implements CustomerPreferenceService {

    private final CustomerPreferenceRepository preferenceRepository;
    private final CustomerOwnershipPolicy      ownershipPolicy;
    private final CustomerMapper               mapper;
    private final AuditLogService              auditLogService;

    @Override
    public CustomerPreferenceResponse getMyPreferences(UUID userId) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        CustomerPreference pref = getOrCreate(customer);
        return mapper.toPreferenceResponse(pref);
    }

    @Override
    @Transactional
    public CustomerPreferenceResponse updatePreferences(UUID userId, CustomerPreferenceRequest request) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        CustomerPreference pref = getOrCreate(customer);

        if (request.getLanguage()            != null) pref.setLanguage(request.getLanguage());
        if (request.getCurrency()            != null) pref.setCurrency(request.getCurrency());
        if (request.getMarketingOptIn()      != null) pref.setMarketingOptIn(request.getMarketingOptIn());
        if (request.getEmailNotifications()  != null) pref.setEmailNotifications(request.getEmailNotifications());
        if (request.getSmsNotifications()    != null) pref.setSmsNotifications(request.getSmsNotifications());
        if (request.getPushNotifications()   != null) pref.setPushNotifications(request.getPushNotifications());

        pref = preferenceRepository.save(pref);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CUSTOMER_PREFERENCES_UPDATED)
                .actorPublicId(userId)
                .entityType("CUSTOMER_PREFERENCE")
                .entityPublicId(pref.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toPreferenceResponse(pref);
    }

    private CustomerPreference getOrCreate(Customer customer) {
        return preferenceRepository.findByCustomer_Id(customer.getId())
                .orElseGet(() -> {
                    CustomerPreference pref = CustomerPreference.builder().customer(customer).build();
                    return preferenceRepository.save(pref);
                });
    }
}
