package ecommerce.modules.wishlist.dto.request;

import lombok.Data;

@Data
public class NotificationPreferenceRequest {

    private Boolean notifyOnPriceDrop;

    private Boolean notifyOnRestock;
}
