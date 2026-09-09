package ecommerce.modules.seller.dto.response;

import ecommerce.common.enums.SellerStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SellerStatusHistoryResponse {

    private SellerStatus previousStatus;
    private SellerStatus newStatus;
    private String reason;
    private UUID changedBy;
    private Instant expiresAt;
    private Instant createdAt;
}
