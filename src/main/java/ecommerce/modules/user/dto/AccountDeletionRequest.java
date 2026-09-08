package ecommerce.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountDeletionRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    private boolean confirm;
}
