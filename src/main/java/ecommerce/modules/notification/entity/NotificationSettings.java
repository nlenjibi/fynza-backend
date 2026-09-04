package ecommerce.modules.notification.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "notification_settings", indexes = {
    @Index(name = "idx_notif_settings_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationSettings extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "order_updates", nullable = false)
    @Builder.Default
    private Boolean isOrderUpdates = true;

    @Column(name = "payment_confirmation", nullable = false)
    @Builder.Default
    private Boolean isPaymentConfirmation = true;

    @Column(name = "shipping_updates", nullable = false)
    @Builder.Default
    private Boolean isShippingUpdates = true;

    @Column(name = "promotional_email", nullable = false)
    @Builder.Default
    private Boolean isPromotionalEmail = true;

    @Column(name = "promotional_sms", nullable = false)
    @Builder.Default
    private Boolean isPromotionalSms = false;

    @Column(name = "new_product_alerts", nullable = false)
    @Builder.Default
    private Boolean isNewProductAlerts = true;

    @Column(name = "price_drop_alerts", nullable = false)
    @Builder.Default
    private Boolean isPriceDropAlerts = true;

    @Column(name = "wishlist_updates", nullable = false)
    @Builder.Default
    private Boolean isWishlistUpdates = true;

    @Column(name = "review_requests", nullable = false)
    @Builder.Default
    private Boolean isReviewRequests = true;

    @Column(name = "newsletter", nullable = false)
    @Builder.Default
    private Boolean isNewsletter = false;

    @Column(name = "browser_push", nullable = false)
    @Builder.Default
    private Boolean isBrowserPush = true;

    @Column(name = "app_push", nullable = false)
    @Builder.Default
    private Boolean isAppPush = true;
}
