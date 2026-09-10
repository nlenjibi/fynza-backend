package ecommerce.modules.category.dto.response;

import ecommerce.modules.category.enums.CategoryStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CategoryStatusHistoryResponse {
    UUID publicId;
    CategoryStatus previousStatus;
    CategoryStatus newStatus;
    String reason;
    UUID changedBy;
    Instant createdAt;
}
