package ecommerce.modules.category.dto.request;

import ecommerce.modules.category.enums.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAttributeDefinitionRequest {

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Attribute code is required")
    @Size(max = 100)
    private String code;

    @NotNull(message = "Data type is required")
    private AttributeDataType dataType;

    @Size(max = 50)
    private String unit;

    @Builder.Default
    private Boolean required = false;

    @Builder.Default
    private Boolean filterable = false;

    @Builder.Default
    private Boolean searchable = false;

    @Builder.Default
    private Boolean variantDefining = false;

    @Builder.Default
    private Integer sortOrder = 0;
}
