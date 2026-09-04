package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStats {
    private Long totalCategories;
    private Long activeCategories;
    private Long featuredCategories;
    private Long rootCategories;
}
