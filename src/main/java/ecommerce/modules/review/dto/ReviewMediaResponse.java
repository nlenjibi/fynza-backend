package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewMediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMediaResponse {

    private UUID id;
    private String mediaReference;
    private ReviewMediaType mediaType;
    private Integer sortOrder;
}
