package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.ReserveStockRequest;
import ecommerce.modules.inventory.dto.response.InventoryReservationResponse;

import java.util.UUID;

public interface InventoryReservationService {

    InventoryReservationResponse reserve(UUID inventoryPublicId, ReserveStockRequest request);

    InventoryReservationResponse confirm(UUID reservationPublicId);

    InventoryReservationResponse release(UUID reservationPublicId);

    void expireStale();
}
