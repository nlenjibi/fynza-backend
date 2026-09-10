package ecommerce.modules.product.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ProductVariantResponse {

    UUID    id;
    UUID    productId;
    String  sku;
    String  barcode;
    String  variantName;
    String  variantStatus;
    String  size;
    String  color;
    Boolean isActive;
    Instant createdAt;
    Instant updatedAt;
}
