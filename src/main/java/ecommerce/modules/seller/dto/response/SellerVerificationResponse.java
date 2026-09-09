package ecommerce.modules.seller.dto.response;

import ecommerce.modules.seller.enums.VerificationItemStatus;
import ecommerce.modules.seller.enums.VerificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SellerVerificationResponse {

    private UUID publicId;
    private VerificationType verificationType;
    private VerificationItemStatus status;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String rejectionReason;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
