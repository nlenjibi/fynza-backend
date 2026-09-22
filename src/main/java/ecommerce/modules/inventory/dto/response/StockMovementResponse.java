package ecommerce.modules.inventory.dto.response;

import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.MovementType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StockMovementResponse {

    private Long id;
    private Long inventoryId;
    private MovementType movementType;
    private int quantity;
    private int previousQuantity;
    private int newQuantity;
    private String referenceType;
    private UUID referenceId;
    private String reason;
    private UUID performedBy;
    private Instant createdAt;

    public static StockMovementResponse from(StockMovement m) {
        return StockMovementResponse.builder()
                .id(m.getId())
                .inventoryId(m.getInventoryId())
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .previousQuantity(m.getPreviousQuantity())
                .newQuantity(m.getNewQuantity())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .reason(m.getReason())
                .performedBy(m.getPerformedBy())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
