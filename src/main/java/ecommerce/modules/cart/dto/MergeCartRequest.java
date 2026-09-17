package ecommerce.modules.cart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeCartRequest {

    @NotBlank(message = "guestCartToken is required")
    private String guestCartToken;
}
