package ecommerce.modules.inventory.service.impl;

import ecommerce.modules.inventory.dto.request.CreateInventoryLocationRequest;
import ecommerce.modules.inventory.dto.response.InventoryLocationResponse;
import ecommerce.modules.inventory.entity.InventoryLocation;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InventoryOwnershipException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryLocationRepository;
import ecommerce.modules.inventory.service.InventoryLocationService;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
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
public class InventoryLocationServiceImpl implements InventoryLocationService {

    private final InventoryLocationRepository locationRepository;
    private final SellerRepository            sellerRepository;

    @Override
    @Transactional
    public InventoryLocationResponse createLocation(CreateInventoryLocationRequest request, UUID userId) {
        Seller seller = resolveSeller(userId);

        if (locationRepository.existsBySellerIdAndCode(seller.getId(), request.getCode())) {
            throw new InvalidInventoryOperationException(
                    "Location code '" + request.getCode() + "' already exists for this seller");
        }

        InventoryLocation location = InventoryLocation.builder()
                .sellerId(seller.getId())
                .storeId(request.getStoreId())
                .name(request.getName())
                .code(request.getCode())
                .locationType(request.getLocationType())
                .build();

        return InventoryLocationResponse.from(locationRepository.save(location));
    }

    @Override
    @Transactional
    public InventoryLocationResponse updateLocation(UUID publicId, CreateInventoryLocationRequest request, UUID userId) {
        Seller seller = resolveSeller(userId);
        InventoryLocation location = findAndVerifyOwnership(publicId, seller.getId());

        location.setName(request.getName());
        location.setLocationType(request.getLocationType());
        if (request.getStoreId() != null) location.setStoreId(request.getStoreId());

        return InventoryLocationResponse.from(locationRepository.save(location));
    }

    @Override
    @Transactional
    public void deleteLocation(UUID publicId, UUID userId) {
        Seller seller = resolveSeller(userId);
        InventoryLocation location = findAndVerifyOwnership(publicId, seller.getId());
        location.setIsActive(false);
        location.setStatus("INACTIVE");
        locationRepository.save(location);
    }

    @Override
    public List<InventoryLocationResponse> getSellerLocations(UUID userId) {
        Seller seller = resolveSeller(userId);
        return locationRepository.findBySellerIdAndIsActiveTrue(seller.getId())
                .stream()
                .map(InventoryLocationResponse::from)
                .toList();
    }

    private Seller resolveSeller(UUID userId) {
        return sellerRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new InventoryOwnershipException("No seller account found for user " + userId));
    }

    private InventoryLocation findAndVerifyOwnership(UUID publicId, Long sellerId) {
        InventoryLocation location = locationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory location not found: " + publicId));
        if (!location.getSellerId().equals(sellerId)) {
            throw new InventoryOwnershipException("Location does not belong to this seller");
        }
        return location;
    }
}
