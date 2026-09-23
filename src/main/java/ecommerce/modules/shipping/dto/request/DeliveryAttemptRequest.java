package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class DeliveryAttemptRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String failureReason;

    private String location;

    private String notes;

    private Instant nextAttemptAt;
}
