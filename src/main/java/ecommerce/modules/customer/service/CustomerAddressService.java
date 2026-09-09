package ecommerce.modules.customer.service;

import ecommerce.modules.customer.dto.request.CustomerAddressRequest;
import ecommerce.modules.customer.dto.response.CustomerAddressResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressService {

    List<CustomerAddressResponse> getMyAddresses(UUID userId);

    CustomerAddressResponse addAddress(UUID userId, CustomerAddressRequest request);

    CustomerAddressResponse updateAddress(UUID userId, UUID addressPublicId, CustomerAddressRequest request);

    void deleteAddress(UUID userId, UUID addressPublicId);

    CustomerAddressResponse setDefaultAddress(UUID userId, UUID addressPublicId);
}
