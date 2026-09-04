package ecommerce.modules.notification.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "notification_preference_activities", indexes = {
    @Index(name = "idx_pref_user", columnList = "user_id"),
    @Index(name = "idx_pref_setting", columnList = "setting_name"),
    @Index(name = "idx_pref_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationPreferenceActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "setting_name", nullable = false, length = 50)
    private String settingName;

    @Column(name = "old_value", length = 50)
    private String oldValue;

    @Column(name = "new_value", length = 50)
    private String newValue;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    public enum SettingName {
        ORDER_UPDATES,
        PAYMENT_CONFIRMATION,
        SHIPPING_UPDATES,
        PROMOTIONAL_EMAIL,
        PROMOTIONAL_SMS,
        NEW_PRODUCT_ALERTS,
        PRICE_DROP_ALERTS,
        WISHLIST_UPDATES,
        REVIEW_REQUESTS,
        NEWSLETTER,
        BROWSER_PUSH,
        APP_PUSH
    }

    public enum Channel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
}
