package ecommerce.modules.category.dto.request;

import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySearchRequest {

    private String query;
    private CategoryStatus status;
    private CategoryVisibility visibility;
    private Long taxonomyId;
    private UUID parentPublicId;
    private Boolean isActive;
}
