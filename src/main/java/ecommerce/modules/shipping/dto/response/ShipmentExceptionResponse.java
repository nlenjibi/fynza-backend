package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShipmentException;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionStatus;
import ecommerce.modules.shipping.enums.ExceptionType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ShipmentExceptionResponse {
    UUID publicId;
    UUID shipmentId;
    ExceptionType type;
    ExceptionSeverity severity;
    String description;
    ExceptionStatus status;
    Instant detectedAt;
    Instant resolvedAt;
    String resolvedBy;
    String resolutionNotes;
    Instant createdAt;

    public static ShipmentExceptionResponse from(ShipmentException e) {
        return ShipmentExceptionResponse.builder()
                .publicId(e.getPublicId())
                .shipmentId(e.getShipmentId())
                .type(e.getType())
                .severity(e.getSeverity())
                .description(e.getDescription())
                .status(e.getStatus())
                .detectedAt(e.getDetectedAt())
                .resolvedAt(e.getResolvedAt())
                .resolvedBy(e.getResolvedBy())
                .resolutionNotes(e.getResolutionNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
