package ecommerce.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for bulk review actions")
public class BulkReviewActionRequest {

    @NotEmpty(message = "Review IDs are required")
    @Schema(description = "List of review IDs to perform action on", example = "[1, 2, 3]")
    private List<UUID> ids;

    @Schema(description = "Reason for rejection (required for reject action)", example = "Does not meet guidelines")
    private String reason;
}
