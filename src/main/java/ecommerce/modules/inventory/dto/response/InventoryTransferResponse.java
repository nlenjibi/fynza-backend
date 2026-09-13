package ecommerce.modules.inventory.dto.response;

import ecommerce.modules.inventory.entity.InventoryTransfer;
import ecommerce.modules.inventory.enums.TransferStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryTransferResponse {

    private UUID publicId;
    private Long sourceLocationId;
    private Long destinationLocationId;
    private TransferStatus status;
    private UUID requestedBy;
    private UUID approvedBy;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryTransferResponse from(InventoryTransfer t) {
        return InventoryTransferResponse.builder()
                .publicId(t.getPublicId())
                .sourceLocationId(t.getSourceLocationId())
                .destinationLocationId(t.getDestinationLocationId())
                .status(t.getStatus())
                .requestedBy(t.getRequestedBy())
                .approvedBy(t.getApprovedBy())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
