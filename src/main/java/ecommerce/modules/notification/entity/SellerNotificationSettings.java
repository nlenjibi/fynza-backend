package ecommerce.modules.notification.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.modules.user.entity.SellerProfile;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "seller_notification_settings", indexes = {
    @Index(name = "idx_seller_notif_seller", columnList = "seller_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SellerNotificationSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private SellerProfile seller;

    @Column(name = "new_orders", nullable = false)
    @Builder.Default
    private Boolean newOrders = true;

    @Column(name = "order_updates", nullable = false)
    @Builder.Default
    private Boolean orderUpdates = true;

    @Column(name = "customer_messages", nullable = false)
    @Builder.Default
    private Boolean customerMessages = true;

    @Column(name = "stock_alerts", nullable = false)
    @Builder.Default
    private Boolean stockAlerts = true;

    @Column(name = "payment_updates", nullable = false)
    @Builder.Default
    private Boolean paymentUpdates = true;

    @Column(name = "refund_requests", nullable = false)
    @Builder.Default
    private Boolean refundRequests = true;

    @Column(name = "promotional_emails", nullable = false)
    @Builder.Default
    private Boolean promotionalEmails = false;
}
