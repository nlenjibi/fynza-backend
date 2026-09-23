package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnFraudSignalType;
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
public class ReturnFraudSignalResponse {

    private Long id;
    private UUID returnId;
    private UUID customerId;
    private ReturnFraudSignalType signalType;
    private String description;
    private Integer severity;
    private Instant detectedAt;
    private Instant createdAt;
}
