package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID customerId;
    private UUID productId;
    private UUID variantId;
    private UUID storeId;
    private UUID sellerId;
    private UUID orderId;
    private UUID orderItemId;
    private ReviewStatus status;
    private Integer rating;
    private String title;
    private String body;
    private Boolean verifiedPurchase;
    private Instant editedAt;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long helpfulCount;
    private Long notHelpfulCount;
    private List<ReviewMediaResponse> media;
    private SellerResponseDTO sellerResponse;
}
