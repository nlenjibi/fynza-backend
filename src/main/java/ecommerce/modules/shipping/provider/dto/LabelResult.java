package ecommerce.modules.shipping.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LabelResult {
    private String labelUrl;
    private String carrierLabelId;
    private String format;
    private Instant expiresAt;
}
