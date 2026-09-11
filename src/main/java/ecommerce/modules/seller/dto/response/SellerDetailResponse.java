package ecommerce.modules.seller.dto.response;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.enums.SellerType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SellerDetailResponse {

    private UUID publicId;
    private String sellerNumber;
    private String displayName;
    private SellerType sellerType;
    private SellerStatus status;
    private SellerBusinessResponse business;
    private List<SellerVerificationResponse> verifications;
    private Instant createdAt;
    private Instant updatedAt;
}
