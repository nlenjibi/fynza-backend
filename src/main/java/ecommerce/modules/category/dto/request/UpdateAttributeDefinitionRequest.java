package ecommerce.modules.category.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAttributeDefinitionRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String unit;

    private Boolean required;
    private Boolean filterable;
    private Boolean searchable;
    private Boolean variantDefining;
    private Integer sortOrder;
    private Boolean isActive;
}
