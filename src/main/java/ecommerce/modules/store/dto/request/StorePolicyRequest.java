package ecommerce.modules.store.dto.request;

import ecommerce.modules.store.enums.StorePolicyStatus;
import ecommerce.modules.store.enums.StorePolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class StorePolicyRequest {

    @NotNull
    private StorePolicyType type;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    private StorePolicyStatus status;

    private Instant effectiveFrom;
}
