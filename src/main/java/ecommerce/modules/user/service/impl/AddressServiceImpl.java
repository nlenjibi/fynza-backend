package ecommerce.modules.user.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.user.dto.AddressDto;
import ecommerce.modules.user.dto.AddressRequest;
import ecommerce.modules.user.entity.Address;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.mapper.AddressMapper;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AddressMapper addressMapper;

    private static final int MAX_ADDRESSES_PER_USER = 10;
    private static final String ADDRESS_NOT_FOUND = "Address not found with id: ";
    private static final String USER_NOT_FOUND = "User not found with id: ";
    private static final String MAX_ADDRESSES_EXCEEDED = "Maximum number of addresses (10) exceeded for this user";

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getAddressesByUserId(UUID userId) {
        log.debug("Fetching all addresses for user: {}", userId);
        return addressRepository.findByUserId(userId).stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDto getAddressById(UUID userId, UUID addressId) {
        log.debug("Fetching address {} for user {}", addressId, userId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        return addressMapper.toDto(address);
    }

    @Override
    @Transactional
    public AddressDto createAddress(UUID userId, AddressRequest request) {
        log.debug("Creating new address for user: {}", userId);

        // Check address limit
        List<Address> existingAddresses = addressRepository.findByUserId(userId);
        if (existingAddresses.size() >= MAX_ADDRESSES_PER_USER) {
            throw new BadRequestException(MAX_ADDRESSES_EXCEEDED);
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));

        // Map and set user
        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        // Handle default address logic
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
            address.setIsDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address created with id: {} for user: {}", savedAddress.getId(), userId);

        return addressMapper.toDto(savedAddress);
    }

    @Override
    @Transactional
    public AddressDto updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        log.debug("Updating address {} for user {}", addressId, userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        // Handle default address logic
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
        }

        addressMapper.updateEntity(address, request);
        
        // Re-apply default if needed
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            address.setIsDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address {} updated for user: {}", addressId, userId);

        return addressMapper.toDto(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        log.debug("Deleting address {} for user {}", addressId, userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        addressRepository.delete(address);
        log.info("Address {} deleted for user: {}", addressId, userId);
    }

    @Override
    @Transactional
    public AddressDto setDefaultAddress(UUID userId, UUID addressId) {
        log.debug("Setting address {} as default for user {}", addressId, userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(ADDRESS_NOT_FOUND + addressId);
        }

        // Clear existing defaults
        addressRepository.clearDefaultByUserId(userId);

        // Set this address as default
        address.setIsDefault(true);
        Address updatedAddress = addressRepository.save(address);
        
        log.info("Address {} set as default for user: {}", addressId, userId);

        return addressMapper.toDto(updatedAddress);
    }
}
