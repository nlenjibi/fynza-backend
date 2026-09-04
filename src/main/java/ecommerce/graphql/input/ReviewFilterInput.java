package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFilterInput {
    private UUID productId;
    private UUID customerId;
    private Integer rating;
    private Integer minRating;
    private Integer maxRating;
    private Boolean verifiedPurchase;
    private Boolean approved;
    private Boolean withImages;
    private String searchText;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Boolean positiveOnly;
    private Boolean negativeOnly;
    private Boolean needsAttention;
}
