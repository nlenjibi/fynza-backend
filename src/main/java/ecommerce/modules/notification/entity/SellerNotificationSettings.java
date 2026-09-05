package ecommerce.modules.notification.entity;

import ecommerce.modules.user.entity.SellerProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_notification_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerNotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private SellerProfile seller;

    @Column(name = "new_orders")
    @Builder.Default
    private Boolean newOrders = true;

    @Column(name = "order_updates")
    @Builder.Default
    private Boolean orderUpdates = true;

    @Column(name = "customer_messages")
    @Builder.Default
    private Boolean customerMessages = true;

    @Column(name = "stock_alerts")
    @Builder.Default
    private Boolean stockAlerts = true;

    @Column(name = "payment_updates")
    @Builder.Default
    private Boolean paymentUpdates = true;

    @Column(name = "refund_requests")
    @Builder.Default
    private Boolean refundRequests = true;

    @Column(name = "promotional_emails")
    @Builder.Default
    private Boolean promotionalEmails = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
