package ecommerce.modules.pricing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulePriceRequest {

    @NotNull(message = "validFrom is required")
    private Instant validFrom;

    private Instant validUntil;
}
