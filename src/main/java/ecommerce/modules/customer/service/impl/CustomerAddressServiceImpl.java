package ecommerce.modules.customer.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerAddressRequest;
import ecommerce.modules.customer.dto.response.CustomerAddressResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerAddress;
import ecommerce.modules.customer.enums.CustomerAddressType;
import ecommerce.modules.customer.exception.AddressNotFoundException;
import ecommerce.modules.customer.exception.AddressOwnershipException;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.policy.CustomerOwnershipPolicy;
import ecommerce.modules.customer.repository.CustomerAddressRepository;
import ecommerce.modules.customer.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private static final int MAX_ADDRESSES = 10;

    private final CustomerAddressRepository addressRepository;
    private final CustomerOwnershipPolicy   ownershipPolicy;
    private final CustomerMapper            mapper;
    private final AuditLogService           auditLogService;

    @Override
    public List<CustomerAddressResponse> getMyAddresses(UUID userId) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        return addressRepository.findByCustomer_IdAndIsActiveTrue(customer.getId())
                .stream()
                .map(mapper::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomerAddressResponse addAddress(UUID userId, CustomerAddressRequest request) {
        Customer customer = ownershipPolicy.resolveOwn(userId);

        int count = addressRepository.countByCustomer_IdAndIsActiveTrue(customer.getId());
        if (count >= MAX_ADDRESSES) {
            throw new BadRequestException("Maximum of " + MAX_ADDRESSES + " addresses allowed");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByCustomerId(customer.getId());
        }

        CustomerAddress address = buildAddress(request, customer);
        address = addressRepository.save(address);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CUSTOMER_ADDRESS_ADDED)
                .actorPublicId(userId)
                .entityType("CUSTOMER_ADDRESS")
                .entityPublicId(address.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toAddressResponse(address);
    }

    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(UUID userId, UUID addressPublicId, CustomerAddressRequest request) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        CustomerAddress address = resolveAndVerifyOwnership(addressPublicId, customer.getId());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultByCustomerId(customer.getId());
        }

        applyUpdates(address, request);
        address = addressRepository.save(address);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CUSTOMER_ADDRESS_UPDATED)
                .actorPublicId(userId)
                .entityType("CUSTOMER_ADDRESS")
                .entityPublicId(address.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toAddressResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressPublicId) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        CustomerAddress address = resolveAndVerifyOwnership(addressPublicId, customer.getId());

        address.setIsActive(false);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            address.setIsDefault(false);
        }
        addressRepository.save(address);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CUSTOMER_ADDRESS_DELETED)
                .actorPublicId(userId)
                .entityType("CUSTOMER_ADDRESS")
                .entityPublicId(addressPublicId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());
    }

    @Override
    @Transactional
    public CustomerAddressResponse setDefaultAddress(UUID userId, UUID addressPublicId) {
        Customer customer = ownershipPolicy.resolveOwn(userId);
        CustomerAddress address = resolveAndVerifyOwnership(addressPublicId, customer.getId());

        addressRepository.clearDefaultByCustomerId(customer.getId());
        address.setIsDefault(true);
        address = addressRepository.save(address);

        return mapper.toAddressResponse(address);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CustomerAddress resolveAndVerifyOwnership(UUID addressPublicId, Long customerId) {
        CustomerAddress address = addressRepository.findByPublicId(addressPublicId)
                .orElseThrow(() -> new AddressNotFoundException(addressPublicId));
        if (!Boolean.TRUE.equals(address.getIsActive())) {
            throw new AddressNotFoundException(addressPublicId);
        }
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new AddressOwnershipException();
        }
        return address;
    }

    private CustomerAddress buildAddress(CustomerAddressRequest req, Customer customer) {
        return CustomerAddress.builder()
                .customer(customer)
                .recipientName(req.getRecipientName())
                .phoneNumber(req.getPhoneNumber())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .region(req.getRegion())
                .country(req.getCountry())
                .postalCode(req.getPostalCode())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .addressType(req.getAddressType() != null ? req.getAddressType() : CustomerAddressType.HOME)
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();
    }

    private void applyUpdates(CustomerAddress address, CustomerAddressRequest req) {
        if (req.getRecipientName() != null) address.setRecipientName(req.getRecipientName());
        if (req.getPhoneNumber()   != null) address.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddressLine1()  != null) address.setAddressLine1(req.getAddressLine1());
        if (req.getAddressLine2()  != null) address.setAddressLine2(req.getAddressLine2());
        if (req.getCity()          != null) address.setCity(req.getCity());
        if (req.getRegion()        != null) address.setRegion(req.getRegion());
        if (req.getCountry()       != null) address.setCountry(req.getCountry());
        if (req.getPostalCode()    != null) address.setPostalCode(req.getPostalCode());
        if (req.getLatitude()      != null) address.setLatitude(req.getLatitude());
        if (req.getLongitude()     != null) address.setLongitude(req.getLongitude());
        if (req.getAddressType()   != null) address.setAddressType(req.getAddressType());
        if (req.getIsDefault()     != null) address.setIsDefault(req.getIsDefault());
    }
}
