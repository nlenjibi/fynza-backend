package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnDispositionType;
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
public class ReturnDispositionResponse {

    private UUID publicId;
    private UUID returnId;
    private UUID returnItemId;
    private ReturnDispositionType type;
    private Integer quantity;
    private String locationId;
    private String reason;
    private UUID processedBy;
    private Instant processedAt;
    private Instant createdAt;
}
