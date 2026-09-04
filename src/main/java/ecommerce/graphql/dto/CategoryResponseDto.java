package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.category.dto.CategoryResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter


public class CategoryResponseDto {
    private List<CategoryResponse> content;
    private PaginatedResponse<CategoryResponse> pageInfo;
}
