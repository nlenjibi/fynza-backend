package ecommerce.modules.customer.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CustomerStatusRequest {

    @Size(max = 500)
    private String reason;

    private Instant expiresAt;
}
