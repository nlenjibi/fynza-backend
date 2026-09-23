package ecommerce.modules.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull
    private UUID productId;

    private UUID variantId;

    private UUID storeId;

    @NotNull
    private UUID orderId;

    @NotNull
    private UUID orderItemId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 5000)
    private String body;
}
