package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ReviewResponseDto {
    private List<ReviewResponse> content;
    private PaginatedResponse<ReviewResponse> pageInfo;
}
