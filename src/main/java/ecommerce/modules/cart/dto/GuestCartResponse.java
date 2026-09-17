package ecommerce.modules.cart.dto;

import lombok.Builder;
import lombok.Value;
import java.util.UUID;

@Value
@Builder
public class GuestCartResponse {
    UUID cartId;
    String cartToken;
}
