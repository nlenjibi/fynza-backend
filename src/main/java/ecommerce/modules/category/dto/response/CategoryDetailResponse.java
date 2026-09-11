package ecommerce.modules.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDetailResponse {
    UUID publicId;
    Long taxonomyId;
    UUID parentCategoryPublicId;
    String name;
    String slug;
    String description;
    CategoryStatus status;
    CategoryVisibility visibility;
    Integer sortOrder;
    String mediaId;
    Boolean isActive;
    Instant createdAt;
    Instant updatedAt;
    List<AttributeDefinitionResponse> attributes;
    List<CategorySummaryResponse> children;
}
