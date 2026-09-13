package ecommerce.modules.inventory.dto.response;

import ecommerce.modules.inventory.entity.InventoryReservation;
import ecommerce.modules.inventory.enums.InventoryReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryReservationResponse {

    private UUID publicId;
    private Long inventoryId;
    private UUID orderId;
    private int quantity;
    private InventoryReservationStatus status;
    private Instant reservedAt;
    private Instant expiresAt;
    private Instant releasedAt;

    public static InventoryReservationResponse from(InventoryReservation r) {
        return InventoryReservationResponse.builder()
                .publicId(r.getPublicId())
                .inventoryId(r.getInventoryId())
                .orderId(r.getOrderId())
                .quantity(r.getQuantity())
                .status(r.getStatus())
                .reservedAt(r.getReservedAt())
                .expiresAt(r.getExpiresAt())
                .releasedAt(r.getReleasedAt())
                .build();
    }
}
