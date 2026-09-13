package ecommerce.modules.category.dto.request;

import ecommerce.modules.category.enums.CategoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 150, message = "Slug must not exceed 150 characters")
    private String slug;

    private Long taxonomyId;

    private UUID parentCategoryPublicId;

    @Builder.Default
    private CategoryVisibility visibility = CategoryVisibility.PUBLIC;

    @Builder.Default
    private Integer sortOrder = 0;

    private String mediaId;
}
