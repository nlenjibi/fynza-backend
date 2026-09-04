package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateInput {
    private Integer rating;
    private String title;
    private String comment;
    private List<String> pros;
    private List<String> cons;
}
