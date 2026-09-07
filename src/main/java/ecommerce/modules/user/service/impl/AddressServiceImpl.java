package ecommerce.modules.user.service.impl;

import ecommerce.common.enums.AddressType;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.user.dto.AddressDto;
import ecommerce.modules.user.dto.AddressRequest;
import ecommerce.modules.user.entity.Address;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADDRESSES_PER_USER = 10;
    private static final String ADDRESS_NOT_FOUND = "Address not found with id: ";
    private static final String USER_NOT_FOUND = "User not found with id: ";
    private static final String MAX_ADDRESSES_EXCEEDED = "Maximum number of addresses (10) exceeded for this user";

    private AddressDto toAddressDto(Address address) {
        LocalDateTime createdAt = address.getCreatedAt() != null
                ? LocalDateTime.ofInstant(address.getCreatedAt(), ZoneId.systemDefault()) : null;
        LocalDateTime updatedAt = address.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(address.getUpdatedAt(), ZoneId.systemDefault()) : null;
        return AddressDto.builder()
                .id(address.getPublicId())
                .label(address.getLabel())
                .addressType(address.getAddressType())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private Address toAddress(AddressRequest request) {
        AddressType type = null;
        if (request.getType() != null) {
            try {
                type = AddressType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return Address.builder()
                .label(request.getLabel())
                .addressType(type)
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();
    }

    private void updateAddressFields(Address address, AddressRequest request) {
        if (request.getLabel() != null) address.setLabel(request.getLabel());
        if (request.getStreetAddress() != null) address.setStreetAddress(request.getStreetAddress());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getType() != null) {
            try {
                address.setAddressType(AddressType.valueOf(request.getType().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.getIsDefault() != null) address.setIsDefault(request.getIsDefault());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getAddressesByUserId(UUID userId) {
        log.debug("Fetching all addresses for user: {}", userId);
        return addressRepository.findByUser_PublicId(userId).stream()
                .map(this::toAddressDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDto getAddressById(UUID userId, UUID addressId) {
        log.debug("Fetching address {} for user {}", addressId, userId);
        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getPublicId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        return toAddressDto(address);
    }

    @Override
    @Transactional
    public AddressDto createAddress(UUID userId, AddressRequest request) {
        log.debug("Creating new address for user: {}", userId);

        List<Address> existingAddresses = addressRepository.findByUser_PublicId(userId);
        if (existingAddresses.size() >= MAX_ADDRESSES_PER_USER) {
            throw new BadRequestException(MAX_ADDRESSES_EXCEEDED);
        }

        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));

        Address address = toAddress(request);
        address.setUser(user);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserPublicId(userId);
            address.setIsDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address created with publicId: {} for user: {}", savedAddress.getPublicId(), userId);

        return toAddressDto(savedAddress);
    }

    @Override
    @Transactional
    public AddressDto updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        log.debug("Updating address {} for user {}", addressId, userId);

        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getPublicId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultByUserPublicId(userId);
        }

        updateAddressFields(address, request);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            address.setIsDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address {} updated for user: {}", addressId, userId);

        return toAddressDto(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        log.debug("Deleting address {} for user {}", addressId, userId);

        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getPublicId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        addressRepository.delete(address);
        log.info("Address {} deleted for user: {}", addressId, userId);
    }

    @Override
    @Transactional
    public AddressDto setDefaultAddress(UUID userId, UUID addressId) {
        log.debug("Setting address {} as default for user {}", addressId, userId);

        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getPublicId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        addressRepository.clearDefaultByUserPublicId(userId);

        address.setIsDefault(true);
        Address updatedAddress = addressRepository.save(address);

        log.info("Address {} set as default for user: {}", addressId, userId);

        return toAddressDto(updatedAddress);
    }
}
