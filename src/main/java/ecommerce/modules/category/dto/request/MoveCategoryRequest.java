package ecommerce.modules.category.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveCategoryRequest {

    /** null means move to root (no parent) */
    private UUID newParentPublicId;
}
