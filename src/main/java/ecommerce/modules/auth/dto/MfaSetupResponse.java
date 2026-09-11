package ecommerce.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaSetupResponse {
    private String secret;
    private String qrCodeUri;
    private String manualEntryCode;
}
