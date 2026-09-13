package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.CreateInventoryLocationRequest;
import ecommerce.modules.inventory.dto.response.InventoryLocationResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryLocationService {

    InventoryLocationResponse createLocation(CreateInventoryLocationRequest request, UUID userId);

    InventoryLocationResponse updateLocation(UUID publicId, CreateInventoryLocationRequest request, UUID userId);

    void deleteLocation(UUID publicId, UUID userId);

    List<InventoryLocationResponse> getSellerLocations(UUID userId);
}
