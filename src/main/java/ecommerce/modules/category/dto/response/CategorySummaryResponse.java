package ecommerce.modules.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategorySummaryResponse {
    UUID publicId;
    Long taxonomyId;
    String name;
    String slug;
    String description;
    CategoryStatus status;
    CategoryVisibility visibility;
    Integer sortOrder;
    String mediaId;
    Boolean isActive;
    Long productCount;
    Long activeProductCount;
    Long childCategoryCount;
    Instant createdAt;
    Instant updatedAt;
}
