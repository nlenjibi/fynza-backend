package ecommerce.modules.authz.constant;

/**
 * Catalogue of every permission code in Fynza, matching the DB seed in 007-authz-permissions.sql.
 */
public final class FynzaPermissions {

    private FynzaPermissions() {}

    // ── Product ───────────────────────────────────────────────────────────────
    public static final String PRODUCT_CREATE  = "product.create";
    public static final String PRODUCT_READ    = "product.read";
    public static final String PRODUCT_UPDATE  = "product.update";
    public static final String PRODUCT_DELETE  = "product.delete";
    public static final String PRODUCT_APPROVE = "product.approve";

    // ── User ──────────────────────────────────────────────────────────────────
    public static final String USER_VIEW    = "user.view";
    public static final String USER_SUSPEND = "user.suspend";
    public static final String USER_MANAGE  = "user.manage";

    // ── Store ─────────────────────────────────────────────────────────────────
    public static final String STORE_VIEW   = "store.view";
    public static final String STORE_MANAGE = "store.manage";

    // ── Order ─────────────────────────────────────────────────────────────────
    public static final String ORDER_VIEW   = "order.view";
    public static final String ORDER_MANAGE = "order.manage";
    public static final String ORDER_REFUND = "order.refund";

    // ── Payment ───────────────────────────────────────────────────────────────
    public static final String PAYMENT_VIEW   = "payment.view";
    public static final String PAYMENT_REFUND = "payment.refund";

    // ── Role ──────────────────────────────────────────────────────────────────
    public static final String ROLE_ASSIGN = "role.assign";
    public static final String ROLE_REVOKE = "role.revoke";
    public static final String ROLE_MANAGE = "role.manage";

    // ── Permission ────────────────────────────────────────────────────────────
    public static final String PERMISSION_GRANT  = "permission.grant";
    public static final String PERMISSION_REVOKE = "permission.revoke";

    // ── Report ────────────────────────────────────────────────────────────────
    public static final String REPORT_VIEW   = "report.view";
    public static final String REPORT_EXPORT = "report.export";

    // ── Category ──────────────────────────────────────────────────────────────
    public static final String CATEGORY_MANAGE = "category.manage";

    // ── Inventory ─────────────────────────────────────────────────────────────
    public static final String INVENTORY_VIEW   = "inventory.view";
    public static final String INVENTORY_MANAGE = "inventory.manage";

    // ── Customer ──────────────────────────────────────────────────────────────
    public static final String CUSTOMER_READ_OWN    = "customer.read.own";
    public static final String CUSTOMER_UPDATE_OWN  = "customer.update.own";
    public static final String ADDRESS_READ_OWN     = "address.read.own";
    public static final String ADDRESS_CREATE_OWN   = "address.create.own";
    public static final String ADDRESS_UPDATE_OWN   = "address.update.own";
    public static final String ADDRESS_DELETE_OWN   = "address.delete.own";
    public static final String CUSTOMER_READ        = "customer.read";
    public static final String CUSTOMER_MANAGE      = "customer.manage";
    public static final String CUSTOMER_SUSPEND     = "customer.suspend";
    public static final String CUSTOMER_ACTIVATE    = "customer.activate";
    public static final String CUSTOMER_BLOCK       = "customer.block";
}
