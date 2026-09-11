package ecommerce.modules.seller.dto.response;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.enums.SellerType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SellerSummaryResponse {

    private UUID publicId;
    private String sellerNumber;
    private String displayName;
    private SellerType sellerType;
    private SellerStatus status;
    private Instant createdAt;
}
