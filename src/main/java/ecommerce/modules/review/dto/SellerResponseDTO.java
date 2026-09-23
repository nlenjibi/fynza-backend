package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.SellerResponseStatus;
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
public class SellerResponseDTO {

    private UUID id;
    private UUID sellerId;
    private String body;
    private SellerResponseStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
