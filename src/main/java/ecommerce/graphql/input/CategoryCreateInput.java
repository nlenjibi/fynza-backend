package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateInput {
    private String name;
    private String description;
    private UUID parentCategoryId;
    private String image;
    private String slug;
    private Boolean featured = false;
    private Boolean isActive = true;
}
