package ecommerce.modules.product.dto.response;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.enums.ProductType;
import ecommerce.common.enums.ProductVisibility;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ProductResponse {

    UUID    id;
    String  productNumber;
    Long    storeId;
    Long    sellerId;
    String  name;
    String  slug;
    String  brand;
    String  sku;
    String  description;
    ProductType       productType;
    ProductStatus     status;
    ProductVisibility visibility;
    Instant createdAt;
    Instant updatedAt;
}
