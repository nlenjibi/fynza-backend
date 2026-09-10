package ecommerce.modules.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.modules.category.enums.CategorySuggestionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategorySuggestionResponse {
    UUID publicId;
    String name;
    String description;
    Long parentCategoryId;
    String reason;
    CategorySuggestionStatus status;
    UUID requestedBy;
    UUID reviewedBy;
    Instant reviewedAt;
    Instant createdAt;
}
