package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagCreateInput {
    private String name;
    private String description;
    private String color;
    private String icon;
    private Boolean isFeatured = false;
}
