package ecommerce.common.cache;

public final class CacheNames {

    private CacheNames() {}

    // Products
    public static final String PRODUCTS               = "products";
    public static final String PRODUCTS_PAGE          = "products-page";
    public static final String PRODUCTS_SEARCH        = "products-search";
    public static final String PRODUCTS_PREDICATE     = "products-predicate";
    public static final String PRODUCTS_FILTER        = "products-filter";
    public static final String PRODUCTS_CATEGORY      = "products-category";
    public static final String PRODUCTS_CATEGORY_NAME = "products-category-name";
    public static final String PRODUCTS_PRICE_RANGE   = "products-price-range";
    public static final String PRODUCTS_DISCOUNTED    = "products-discounted";
    public static final String PRODUCTS_FEATURED      = "products-featured";
    public static final String PRODUCTS_NEW           = "products-new";
    public static final String PRODUCTS_BESTSELLER    = "products-bestseller";
    public static final String PRODUCTS_TOP_RATED     = "products-top-rated";
    public static final String PRODUCTS_TRENDING      = "products-trending";
    public static final String PRODUCTS_STATUS        = "products-status";
    public static final String PRODUCTS_REORDER       = "products-reorder";

    // Categories
    public static final String CATEGORIES        = "categories";
    public static final String CATEGORIES_LIST   = "categories-list";
    public static final String CATEGORIES_PAGED  = "categories-paged";
    public static final String CATEGORIES_SEARCH = "categories-search";
    public static final String CATEGORIES_FILTER = "categories-filter";
    public static final String CATEGORIES_STATS  = "categories-stats";

    // Orders
    public static final String ORDERS           = "orders";
    public static final String ORDER            = "order";
    public static final String ORDER_EXISTS     = "order-exists";
    public static final String USER_ORDERS      = "user-orders";
    public static final String ORDER_STATS      = "order-stats";
    public static final String ORDER_COUNTS     = "order-counts";
    public static final String ORDERS_PREDICATE = "orders-predicate";
    public static final String ORDERS_SEARCH    = "orders-search";
    public static final String ORDERS_FILTER    = "orders-filter";

    // Users
    public static final String USERS           = "users";
    public static final String USERS_PAGE      = "users-page";
    public static final String USERS_SEARCH    = "users-search";
    public static final String USERS_ROLE      = "users-role";
    public static final String USERS_ACTIVE    = "users-active";
    public static final String USERS_PREDICATE = "users-predicate";
    public static final String USER_PRINCIPALS = "userPrincipals";
    public static final String USER_PROFILE    = "user-profile";

    // Reviews
    public static final String REVIEWS                = "reviews";
    public static final String REVIEW                 = "review";
    public static final String REVIEWS_PREDICATE      = "reviews-predicate";
    public static final String REVIEW_STATS           = "review-stats";
    public static final String RATING_DISTRIBUTION    = "rating-distribution";
    public static final String REVIEW_TRENDS          = "review-trends";
    public static final String TOP_RATED_PRODUCTS     = "top-rated-products";
    public static final String MOST_REVIEWED_PRODUCTS = "most-reviewed-products";
    public static final String USER_REVIEWS           = "user-reviews";
    public static final String REVIEW_LISTS           = "review-lists";
    public static final String ADMIN_REVIEWS          = "admin-reviews";

    // Wishlists
    public static final String WISHLIST           = "wishlists";
    public static final String WISHLIST_PAGINATED = "wishlists-paginated";
    public static final String WISHLIST_SUMMARY   = "wishlists-summary";
    public static final String WISHLIST_CHECK     = "wishlists-check";
    public static final String WISHLIST_DROPS     = "wishlists-drops";
    public static final String WISHLIST_ANALYTICS = "wishlists-analytics";

    // Security
    public static final String TOKEN_BLACKLIST     = "tokenBlacklist";
    public static final String STOCK_RESERVATIONS  = "stockReservations";

    // Admin / misc
    public static final String ADMIN_DASHBOARD  = "admin-dashboard";
    public static final String ADMIN_ANALYTICS  = "admin-analytics";
    public static final String SELLER_DASHBOARD = "seller-dashboard";
    public static final String FAQS             = "faqs";
    public static final String SEARCH_FILTERS   = "searchFilters";
    public static final String SETTINGS         = "settings";
}
