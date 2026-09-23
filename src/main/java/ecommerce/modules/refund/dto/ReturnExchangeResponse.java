package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ExchangeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnExchangeResponse {

    private UUID publicId;
    private UUID returnId;
    private UUID originalOrderId;
    private UUID exchangeOrderId;
    private ExchangeStatus status;
    private String requestedItemsDescription;
    private String notes;
    private UUID requestedBy;
    private UUID processedBy;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
