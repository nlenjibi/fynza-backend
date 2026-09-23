package ecommerce.modules.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalateReturnRequest {

    @NotBlank(message = "Escalation reason is required")
    private String reason;
}
