package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.DeliveryAttempt;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class DeliveryAttemptResponse {
    UUID publicId;
    UUID shipmentId;
    int attemptNumber;
    String status;
    String failureReason;
    String location;
    String notes;
    Instant nextAttemptAt;
    Instant createdAt;

    public static DeliveryAttemptResponse from(DeliveryAttempt attempt) {
        return DeliveryAttemptResponse.builder()
                .publicId(attempt.getPublicId())
                .shipmentId(attempt.getShipmentId())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .failureReason(attempt.getFailureReason())
                .location(attempt.getLocation())
                .notes(attempt.getNotes())
                .nextAttemptAt(attempt.getNextAttemptAt())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}
