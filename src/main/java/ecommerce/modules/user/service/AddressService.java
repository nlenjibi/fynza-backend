package ecommerce.modules.user.service;

import ecommerce.modules.user.dto.AddressDto;
import ecommerce.modules.user.dto.AddressRequest;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressDto> getAddressesByUserId(UUID userId);

    AddressDto getAddressById(UUID userId, UUID addressId);

    AddressDto createAddress(UUID userId, AddressRequest request);

    AddressDto updateAddress(UUID userId, UUID addressId, AddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    AddressDto setDefaultAddress(UUID userId, UUID addressId);
}
