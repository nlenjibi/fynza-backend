package ecommerce.modules.seller.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class SellerStatusRequest {

    private String reason;
    private Instant expiresAt;
}
