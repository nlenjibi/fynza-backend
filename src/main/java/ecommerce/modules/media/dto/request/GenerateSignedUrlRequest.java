package ecommerce.modules.media.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSignedUrlRequest {

    @Positive
    @Max(3600)
    @Builder.Default
    private long expirySeconds = 600;
}
