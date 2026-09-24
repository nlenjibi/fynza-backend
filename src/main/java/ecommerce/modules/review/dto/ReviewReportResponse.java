package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewReportReason;
import ecommerce.modules.review.enums.ReviewReportStatus;
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
public class ReviewReportResponse {

    private UUID id;
    private UUID reviewId;
    private ReviewReportReason reason;
    private String description;
    private ReviewReportStatus status;
    private Instant createdAt;
}
