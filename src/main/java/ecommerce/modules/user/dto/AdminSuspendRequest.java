package ecommerce.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminSuspendRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    private Integer durationDays;
}
