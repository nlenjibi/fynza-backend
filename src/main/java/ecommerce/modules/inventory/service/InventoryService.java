package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.AdjustStockRequest;
import ecommerce.modules.inventory.dto.request.CreateInventoryRequest;
import ecommerce.modules.inventory.dto.request.UpdateInventoryRequest;
import ecommerce.modules.inventory.dto.response.AvailabilityResponse;
import ecommerce.modules.inventory.dto.response.InventoryResponse;
import ecommerce.modules.inventory.dto.response.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {

    InventoryResponse createInventory(CreateInventoryRequest request, UUID userId);

    InventoryResponse updateInventory(UUID publicId, UpdateInventoryRequest request, UUID userId);

    InventoryResponse getInventory(UUID publicId, UUID userId);

    Page<InventoryResponse> getSellerInventory(UUID userId, Pageable pageable);

    StockMovementResponse adjustStock(UUID publicId, AdjustStockRequest request, UUID userId);

    AvailabilityResponse getAvailability(UUID productId, UUID variantId);
}
