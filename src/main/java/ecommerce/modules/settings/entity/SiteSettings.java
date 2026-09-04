package ecommerce.modules.settings.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "site_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SiteSettings extends BaseEntity {

    @Column(name = "site_name", length = 255)
    private String siteName;

    @Column(name = "site_email", length = 255)
    private String siteEmail;

    @Column(name = "site_phone", length = 50)
    private String sitePhone;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "America/New_York";

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "free_shipping_threshold", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal freeShippingThreshold = BigDecimal.ZERO;

    @Column(name = "paystack_public_key", length = 500)
    private String paystackPublicKey;

    @Column(name = "paystack_secret_key", length = 500)
    private String paystackSecretKey;

    @Column(name = "enable_cash_on_delivery")
    @Builder.Default
    private Boolean enableCashOnDelivery = true;

    @Column(name = "enable_mobile_money")
    @Builder.Default
    private Boolean enableMobileMoney = true;

    @Column(name = "estimated_delivery_days", length = 20)
    private String estimatedDeliveryDays;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port", length = 10)
    private String smtpPort;

    @Column(name = "smtp_email", length = 255)
    private String smtpEmail;

    @Column(name = "smtp_password", length = 500)
    private String smtpPassword;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "order_notifications")
    @Builder.Default
    private Boolean orderNotifications = true;

    @Column(name = "refund_notifications")
    @Builder.Default
    private Boolean refundNotifications = true;

    @Column(name = "seller_notifications")
    @Builder.Default
    private Boolean sellerNotifications = true;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = true;

    @Column(name = "session_timeout_minutes")
    @Builder.Default
    private Integer sessionTimeoutMinutes = 30;

    @Column(name = "login_notifications")
    @Builder.Default
    private Boolean loginNotifications = true;

    @Column(name = "enable_order_updates")
    @Builder.Default
    private Boolean enableOrderUpdates = true;

    @Column(name = "enable_payment_confirmation")
    @Builder.Default
    private Boolean enablePaymentConfirmation = true;

    @Column(name = "enable_shipping_updates")
    @Builder.Default
    private Boolean enableShippingUpdates = true;

    @Column(name = "enable_promotional_emails")
    @Builder.Default
    private Boolean enablePromotionalEmails = true;

    @Column(name = "enable_new_product_alerts")
    @Builder.Default
    private Boolean enableNewProductAlerts = true;

    @Column(name = "enable_price_drop_alerts")
    @Builder.Default
    private Boolean enablePriceDropAlerts = true;

    @Column(name = "enable_wishlist_updates")
    @Builder.Default
    private Boolean enableWishlistUpdates = true;

    @Column(name = "enable_review_requests")
    @Builder.Default
    private Boolean enableReviewRequests = true;

    @Column(name = "enable_newsletter")
    @Builder.Default
    private Boolean enableNewsletter = true;

    @Column(name = "enable_promotional_sms")
    @Builder.Default
    private Boolean enablePromotionalSms = false;

    @Column(name = "enable_browser_notifications")
    @Builder.Default
    private Boolean enableBrowserNotifications = true;

    @Column(name = "enable_app_notifications")
    @Builder.Default
    private Boolean enableAppNotifications = true;
}
