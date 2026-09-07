package ecommerce.modules.audit.constant;

/**
 * Catalogue of every audited action in Fynza, grouped by domain.
 * Each constant is stored verbatim in the {@code action} column of {@code audit_log}.
 */
public final class AuditAction {

    private AuditAction() {}

    // ── Authentication ────────────────────────────────────────────────────────
    public static final String LOGIN                    = "LOGIN";
    public static final String LOGOUT                   = "LOGOUT";
    public static final String FAILED_LOGIN             = "FAILED_LOGIN";
    public static final String FAILED_ACCESS            = "FAILED_ACCESS";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";
    public static final String PASSWORD_CHANGED         = "PASSWORD_CHANGED";
    public static final String EMAIL_VERIFIED           = "EMAIL_VERIFIED";
    public static final String OAUTH_LOGIN              = "OAUTH_LOGIN";
    public static final String TOKEN_REFRESHED          = "TOKEN_REFRESHED";
    public static final String ACCOUNT_LOCKED           = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_UNLOCKED         = "ACCOUNT_UNLOCKED";

    // ── User Management ───────────────────────────────────────────────────────
    public static final String USER_REGISTERED          = "USER_REGISTERED";
    public static final String USER_PROFILE_UPDATED     = "USER_PROFILE_UPDATED";
    public static final String USER_ACTIVATED           = "USER_ACTIVATED";
    public static final String USER_DEACTIVATED         = "USER_DEACTIVATED";
    public static final String USER_ROLE_CHANGED        = "USER_ROLE_CHANGED";
    public static final String USER_DELETED             = "USER_DELETED";
    public static final String ADDRESS_ADDED            = "ADDRESS_ADDED";
    public static final String ADDRESS_UPDATED          = "ADDRESS_UPDATED";
    public static final String ADDRESS_DELETED          = "ADDRESS_DELETED";

    // ── Seller Management ─────────────────────────────────────────────────────
    public static final String SELLER_REGISTERED        = "SELLER_REGISTERED";
    public static final String SELLER_APPROVED          = "SELLER_APPROVED";
    public static final String SELLER_REJECTED          = "SELLER_REJECTED";
    public static final String SELLER_SUSPENDED         = "SELLER_SUSPENDED";
    public static final String SELLER_REACTIVATED       = "SELLER_REACTIVATED";
    public static final String SELLER_PROFILE_UPDATED   = "SELLER_PROFILE_UPDATED";
    public static final String STORE_CREATED            = "STORE_CREATED";
    public static final String STORE_UPDATED            = "STORE_UPDATED";

    // ── Product Management ────────────────────────────────────────────────────
    public static final String PRODUCT_CREATED          = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED          = "PRODUCT_UPDATED";
    public static final String PRODUCT_DELETED          = "PRODUCT_DELETED";
    public static final String PRODUCT_ACTIVATED        = "PRODUCT_ACTIVATED";
    public static final String PRODUCT_DEACTIVATED      = "PRODUCT_DEACTIVATED";
    public static final String PRODUCT_APPROVED         = "PRODUCT_APPROVED";
    public static final String PRODUCT_REJECTED         = "PRODUCT_REJECTED";
    public static final String PRODUCT_FEATURED         = "PRODUCT_FEATURED";
    public static final String PRODUCT_UNFEATURED       = "PRODUCT_UNFEATURED";
    public static final String PRODUCT_IMAGE_ADDED      = "PRODUCT_IMAGE_ADDED";
    public static final String PRODUCT_IMAGE_REMOVED    = "PRODUCT_IMAGE_REMOVED";

    // ── Inventory ─────────────────────────────────────────────────────────────
    public static final String STOCK_ADDED              = "STOCK_ADDED";
    public static final String STOCK_REDUCED            = "STOCK_REDUCED";
    public static final String STOCK_RESERVED           = "STOCK_RESERVED";
    public static final String STOCK_RELEASED           = "STOCK_RELEASED";

    // ── Category & Taxonomy ───────────────────────────────────────────────────
    public static final String CATEGORY_CREATED         = "CATEGORY_CREATED";
    public static final String CATEGORY_UPDATED         = "CATEGORY_UPDATED";
    public static final String CATEGORY_DELETED         = "CATEGORY_DELETED";
    public static final String TAG_CREATED              = "TAG_CREATED";
    public static final String TAG_UPDATED              = "TAG_UPDATED";
    public static final String TAG_DELETED              = "TAG_DELETED";

    // ── Pricing & Promotions ──────────────────────────────────────────────────
    public static final String PRICE_UPDATED            = "PRICE_UPDATED";
    public static final String PROMOTION_CREATED        = "PROMOTION_CREATED";
    public static final String PROMOTION_UPDATED        = "PROMOTION_UPDATED";
    public static final String PROMOTION_ACTIVATED      = "PROMOTION_ACTIVATED";
    public static final String PROMOTION_DEACTIVATED    = "PROMOTION_DEACTIVATED";
    public static final String COUPON_CREATED           = "COUPON_CREATED";
    public static final String COUPON_UPDATED           = "COUPON_UPDATED";
    public static final String COUPON_APPLIED           = "COUPON_APPLIED";
    public static final String COUPON_REVOKED           = "COUPON_REVOKED";

    // ── Cart ──────────────────────────────────────────────────────────────────
    public static final String CART_ITEM_ADDED          = "CART_ITEM_ADDED";
    public static final String CART_ITEM_REMOVED        = "CART_ITEM_REMOVED";
    public static final String CART_CLEARED             = "CART_CLEARED";
    public static final String CART_CHECKED_OUT         = "CART_CHECKED_OUT";

    // ── Orders ────────────────────────────────────────────────────────────────
    public static final String ORDER_PLACED             = "ORDER_PLACED";
    public static final String ORDER_CONFIRMED          = "ORDER_CONFIRMED";
    public static final String ORDER_CANCELLED          = "ORDER_CANCELLED";
    public static final String ORDER_SHIPPED            = "ORDER_SHIPPED";
    public static final String ORDER_DELIVERED          = "ORDER_DELIVERED";
    public static final String ORDER_STATUS_CHANGED     = "ORDER_STATUS_CHANGED";
    public static final String ORDER_NOTES_UPDATED      = "ORDER_NOTES_UPDATED";

    // ── Payments ──────────────────────────────────────────────────────────────
    public static final String PAYMENT_INITIATED        = "PAYMENT_INITIATED";
    public static final String PAYMENT_SUCCEEDED        = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED           = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED         = "PAYMENT_REFUNDED";

    // ── Refunds ───────────────────────────────────────────────────────────────
    public static final String REFUND_REQUESTED         = "REFUND_REQUESTED";
    public static final String REFUND_APPROVED          = "REFUND_APPROVED";
    public static final String REFUND_REJECTED          = "REFUND_REJECTED";
    public static final String REFUND_PROCESSED         = "REFUND_PROCESSED";

    // ── Reviews ───────────────────────────────────────────────────────────────
    public static final String REVIEW_SUBMITTED         = "REVIEW_SUBMITTED";
    public static final String REVIEW_APPROVED          = "REVIEW_APPROVED";
    public static final String REVIEW_REJECTED          = "REVIEW_REJECTED";
    public static final String REVIEW_DELETED           = "REVIEW_DELETED";

    // ── Wishlist ──────────────────────────────────────────────────────────────
    public static final String WISHLIST_ITEM_ADDED      = "WISHLIST_ITEM_ADDED";
    public static final String WISHLIST_ITEM_REMOVED    = "WISHLIST_ITEM_REMOVED";

    // ── Admin ─────────────────────────────────────────────────────────────────
    public static final String ADMIN_SETTINGS_UPDATED   = "ADMIN_SETTINGS_UPDATED";
    public static final String REPORT_EXPORTED          = "REPORT_EXPORTED";
    public static final String BULK_ACTION_PERFORMED    = "BULK_ACTION_PERFORMED";
}
