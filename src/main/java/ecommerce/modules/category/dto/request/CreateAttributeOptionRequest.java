package ecommerce.modules.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAttributeOptionRequest {

    @NotBlank(message = "Option value is required")
    @Size(max = 100)
    private String value;

    @Size(max = 100)
    private String label;

    @Builder.Default
    private Integer sortOrder = 0;
}
