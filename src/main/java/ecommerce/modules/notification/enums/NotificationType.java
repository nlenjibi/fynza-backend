package ecommerce.modules.notification.enums;

/**
 * All first-class notification events in the Fynza e-commerce system.
 * Each value maps to one or more NotificationTemplate rows that provide
 * channel-specific content for that event.
 */
public enum NotificationType {

    // ── Orders ─────────────────────────────────────────────────────────────────
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_OUT_FOR_DELIVERY,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_RETURN_REQUESTED,
    ORDER_RETURN_APPROVED,
    ORDER_RETURN_REJECTED,
    ORDER_REFUNDED,

    // ── Payments ────────────────────────────────────────────────────────────────
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    PAYMENT_REFUND_INITIATED,
    PAYMENT_REFUND_PROCESSED,

    // ── Products ────────────────────────────────────────────────────────────────
    PRODUCT_BACK_IN_STOCK,
    PRODUCT_PRICE_DROP,
    PRODUCT_REVIEW_RECEIVED,

    // ── Cart / Wishlist ─────────────────────────────────────────────────────────
    CART_ABANDONED,
    WISHLIST_ITEM_BACK_IN_STOCK,

    // ── Seller ──────────────────────────────────────────────────────────────────
    SELLER_ORDER_RECEIVED,
    SELLER_ORDER_CANCELLED,
    SELLER_PAYMENT_RECEIVED,
    SELLER_PAYOUT_PROCESSED,
    SELLER_PRODUCT_APPROVED,
    SELLER_PRODUCT_REJECTED,
    SELLER_ACCOUNT_APPROVED,
    SELLER_ACCOUNT_SUSPENDED,
    SELLER_LOW_STOCK_ALERT,

    // ── Promotions ──────────────────────────────────────────────────────────────
    COUPON_EXPIRING,
    PROMOTION_LIVE,
    PROMOTION_ENDING,

    // ── Auth / User ─────────────────────────────────────────────────────────────
    USER_EMAIL_VERIFIED,
    USER_PASSWORD_CHANGED,
    USER_LOGIN_NEW_DEVICE,

    // ── Reviews ─────────────────────────────────────────────────────────────────
    REVIEW_FLAGGED,
    REVIEW_APPROVED,
    REVIEW_REJECTED,

    // ── Messages ────────────────────────────────────────────────────────────────
    MESSAGE_RECEIVED,

    // ── System ──────────────────────────────────────────────────────────────────
    SYSTEM_ALERT
}
