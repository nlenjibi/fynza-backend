package ecommerce.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LinkedIdentityResponse {
    private String provider;
    private String displayName;
    private String email;
    private String avatarUrl;
    private Instant linkedAt;
}
