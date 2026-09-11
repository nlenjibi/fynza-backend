package ecommerce.modules.category.dto.request;

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
public class CategorySuggestionRequest {

    @NotBlank(message = "Suggested category name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private UUID parentCategoryPublicId;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;
}
