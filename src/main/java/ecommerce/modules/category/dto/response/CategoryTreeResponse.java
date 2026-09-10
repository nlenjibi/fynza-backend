package ecommerce.modules.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryTreeResponse {
    UUID publicId;
    String name;
    String slug;
    String mediaId;
    CategoryStatus status;
    CategoryVisibility visibility;
    Integer sortOrder;
    List<CategoryTreeResponse> children;
}
