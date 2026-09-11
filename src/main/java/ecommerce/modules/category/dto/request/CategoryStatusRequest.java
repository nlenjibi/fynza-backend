package ecommerce.modules.category.dto.request;

import ecommerce.modules.category.enums.CategoryStatus;
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
public class CategoryStatusRequest {

    @NotNull(message = "Status is required")
    private CategoryStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
