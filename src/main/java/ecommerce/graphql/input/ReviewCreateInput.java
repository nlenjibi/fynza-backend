package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateInput {
    private UUID productId;
    private Integer rating;
    private String title;
    private String comment;
    private List<String> pros;
    private List<String> cons;
    private List<String> images;
}
