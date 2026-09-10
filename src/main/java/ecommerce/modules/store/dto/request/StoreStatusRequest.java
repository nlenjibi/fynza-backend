package ecommerce.modules.store.dto.request;

import ecommerce.modules.store.enums.StoreStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class StoreStatusRequest {

    @NotNull
    private StoreStatus status;

    private String reason;

    private Instant expiresAt;
}
