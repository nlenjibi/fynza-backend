package ecommerce.graphql.resolver.review;

import ecommerce.modules.review.dto.ReviewReportResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GraphQL return type for paginated review report results.
 * Maps to the {@code ReviewReportPage} GraphQL type defined in review.graphqls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportPageResponse {

    private List<ReviewReportResponse> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
