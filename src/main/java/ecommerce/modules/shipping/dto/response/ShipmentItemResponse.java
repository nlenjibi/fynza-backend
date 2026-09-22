package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShipmentItem;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ShipmentItemResponse {
    UUID id;
    UUID orderItemId;
    UUID productId;
    UUID variantId;
    String productName;
    String productSku;
    Integer quantity;

    public static ShipmentItemResponse from(ShipmentItem item) {
        return ShipmentItemResponse.builder()
                .id(item.getPublicId())
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .build();
    }
}
