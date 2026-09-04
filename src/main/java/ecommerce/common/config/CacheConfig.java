package ecommerce.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Unified Cache Configuration
 * 
 * Supports both Caffeine (L1) and Redis (L2) caching based on configuration.
 * 
 * Configuration:
 * - cache.level=caffeine  -> L1 only (default)
 * - cache.level=redis     -> L2 only
 * - cache.level=multi     -> L1 + L2 (recommended for production)
 * 
 * Caches are grouped into three tiers:
 * - SHORT_LIVED: 5-15 minutes (user sessions, stats)
 * - MEDIUM_LIVED: 30-60 minutes (products, categories)
 * - LONG_LIVED: 2-3 hours (configurations)
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // ====== Cache Tier Definitions ======
    public static final String SHORT_LIVED = "short-lived";
    public static final String MEDIUM_LIVED = "medium-lived";
    public static final String LONG_LIVED = "long-lived";

    // TTL configurations (in minutes)
    private static final int SHORT_LIVED_TTL = 15;
    private static final int MEDIUM_LIVED_TTL = 60;
    private static final int LONG_LIVED_TTL = 180;

    // Size configurations
    private static final int SHORT_LIVED_SIZE = 5000;
    private static final int MEDIUM_LIVED_SIZE = 2000;
    private static final int LONG_LIVED_SIZE = 500;

    // ====== Product cache names ======
    public static final String PRODUCTS_CACHE               = "products";
    public static final String PRODUCTS_PAGE_CACHE          = "products-page";
    public static final String PRODUCTS_SEARCH_CACHE        = "products-search";
    public static final String PRODUCTS_PREDICATE_CACHE     = "products-predicate";
    public static final String PRODUCTS_FILTER_CACHE        = "products-filter";
    public static final String PRODUCTS_CATEGORY_CACHE      = "products-category";
    public static final String PRODUCTS_CATEGORY_NAME_CACHE = "products-category-name";
    public static final String PRODUCTS_PRICE_RANGE_CACHE   = "products-price-range";
    public static final String PRODUCTS_DISCOUNTED_CACHE    = "products-discounted";
    public static final String PRODUCTS_FEATURED_CACHE      = "products-featured";
    public static final String PRODUCTS_NEW_CACHE           = "products-new";
    public static final String PRODUCTS_BESTSELLER_CACHE    = "products-bestseller";
    public static final String PRODUCTS_TOP_RATED_CACHE     = "products-top-rated";
    public static final String PRODUCTS_TRENDING_CACHE      = "products-trending";
    public static final String PRODUCTS_STATUS_CACHE        = "products-status";
    public static final String PRODUCTS_REORDER_CACHE       = "products-reorder";

    // ====== Category cache names ======
    public static final String CATEGORIES_CACHE        = "categories";
    public static final String CATEGORIES_LIST_CACHE   = "categories-list";
    public static final String CATEGORIES_PAGED_CACHE  = "categories-paged";
    public static final String CATEGORIES_SEARCH_CACHE = "categories-search";
    public static final String CATEGORIES_FILTER_CACHE = "categories-filter";
    public static final String CATEGORIES_STATS_CACHE  = "categories-stats";

    // ====== Order cache names ======
    public static final String ORDERS_CACHE           = "orders";
    public static final String ORDER_CACHE            = "order";
    public static final String ORDER_EXISTS_CACHE     = "order-exists";
    public static final String USER_ORDERS_CACHE      = "user-orders";
    public static final String ORDER_STATS_CACHE      = "order-stats";
    public static final String ORDER_COUNTS_CACHE     = "order-counts";
    public static final String ORDERS_PREDICATE_CACHE = "orders-predicate";
    public static final String ORDERS_SEARCH_CACHE    = "orders-search";
    public static final String ORDERS_FILTER_CACHE    = "orders-filter";

    // ====== User cache names ======
    public static final String USERS_CACHE           = "users";
    public static final String USERS_PAGE_CACHE      = "users-page";
    public static final String USERS_SEARCH_CACHE    = "users-search";
    public static final String USERS_ROLE_CACHE      = "users-role";
    public static final String USERS_ACTIVE_CACHE    = "users-active";
    public static final String USERS_PREDICATE_CACHE = "users-predicate";
    /** Caches {@link ecommerce.common.security.UserPrincipal}
     *  objects loaded by {@code TokenValidationService} to skip repeated DB hits
     *  on every authenticated request. Evicted explicitly on any security-state change. */
    public static final String USER_PRINCIPALS_CACHE = "userPrincipals";

    // ====== Cart cache names ======
    public static final String CARTS_CACHE = "carts";

    // ====== Review cache names ======
    public static final String REVIEWS_CACHE                = "reviews";
    public static final String REVIEW_CACHE                 = "review";
    public static final String REVIEWS_PREDICATE_CACHE      = "reviews-predicate";
    public static final String REVIEW_STATS_CACHE           = "review-stats";
    public static final String RATING_DISTRIBUTION_CACHE    = "rating-distribution";
    public static final String REVIEW_TRENDS_CACHE          = "review-trends";
    public static final String TOP_RATED_PRODUCTS_CACHE     = "top-rated-products";
    public static final String MOST_REVIEWED_PRODUCTS_CACHE = "most-reviewed-products";
    public static final String USER_REVIEWS_CACHE           = "user-reviews";
    public static final String REVIEW_LISTS_CACHE           = "review-lists";
    public static final String ADMIN_REVIEWS_CACHE          = "admin-reviews";

    // ====== Wishlist cache names ======
    public static final String WISHLIST_CACHE           = "wishlists";
    public static final String WISHLIST_PAGINATED_CACHE = "wishlists-paginated";
    public static final String WISHLIST_SUMMARY_CACHE   = "wishlists-summary";
    public static final String WISHLIST_CHECK_CACHE     = "wishlists-check";
    public static final String WISHLIST_DROPS_CACHE     = "wishlists-drops";
    public static final String WISHLIST_ANALYTICS_CACHE = "wishlists-analytics";

    // ====== Admin / dashboard cache names ======
    public static final String DASHBOARD_CACHE = "admin-dashboard";

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        log.info("Configuring Caffeine cache manager with consolidated cache policies");
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                // --- SHORT_LIVED (5-15 min) - Rapidly changing data ---
                buildCaffeineCache(ORDER_STATS_CACHE,       100, 15),
                buildCaffeineCache(ORDER_COUNTS_CACHE,      500, 15),
                buildCaffeineCache(CARTS_CACHE,             3000, 15),
                buildCaffeineCache(USER_PRINCIPALS_CACHE,    5000, 5),
                buildCaffeineCache("user-profile",           5000, 15),
                buildCaffeineCache("seller-dashboard",        100, 10),

                // --- MEDIUM_LIVED (30-60 min) - Moderately changing data ---
                buildCaffeineCache(PRODUCTS_CACHE,               2000, 60),
                buildCaffeineCache(PRODUCTS_PAGE_CACHE,          1000, 60),
                buildCaffeineCache(PRODUCTS_SEARCH_CACHE,        1000, 60),
                buildCaffeineCache(PRODUCTS_PREDICATE_CACHE,     1000, 60),
                buildCaffeineCache(PRODUCTS_FILTER_CACHE,        1000, 60),
                buildCaffeineCache(PRODUCTS_CATEGORY_CACHE,      1000, 60),
                buildCaffeineCache(PRODUCTS_CATEGORY_NAME_CACHE, 1000, 60),
                buildCaffeineCache(PRODUCTS_PRICE_RANGE_CACHE,   1000, 60),
                buildCaffeineCache(PRODUCTS_STATUS_CACHE,        1000, 60),
                buildCaffeineCache(PRODUCTS_REORDER_CACHE,        500, 30),

                buildCaffeineCache(CATEGORIES_CACHE,        500, 180),
                buildCaffeineCache(CATEGORIES_LIST_CACHE,   500, 180),
                buildCaffeineCache(CATEGORIES_PAGED_CACHE,  500, 180),
                buildCaffeineCache(CATEGORIES_SEARCH_CACHE, 500, 180),
                buildCaffeineCache(CATEGORIES_FILTER_CACHE, 500, 180),

                buildCaffeineCache(ORDERS_CACHE,           2000, 30),
                buildCaffeineCache(ORDER_CACHE,            2000, 30),
                buildCaffeineCache(ORDER_EXISTS_CACHE,     2000, 30),
                buildCaffeineCache(USER_ORDERS_CACHE,      2000, 30),
                buildCaffeineCache(ORDERS_PREDICATE_CACHE, 1000, 30),
                buildCaffeineCache(ORDERS_SEARCH_CACHE,    1000, 30),
                buildCaffeineCache(ORDERS_FILTER_CACHE,    1000, 30),

                buildCaffeineCache(USERS_CACHE,           1000, 60),
                buildCaffeineCache(USERS_PAGE_CACHE,      1000, 60),
                buildCaffeineCache(USERS_SEARCH_CACHE,    1000, 60),
                buildCaffeineCache(USERS_ROLE_CACHE,      1000, 60),
                buildCaffeineCache(USERS_ACTIVE_CACHE,    1000, 60),
                buildCaffeineCache(USERS_PREDICATE_CACHE, 1000, 60),

                buildCaffeineCache(REVIEWS_CACHE,                2000, 60),
                buildCaffeineCache(REVIEW_CACHE,                 1000, 60),
                buildCaffeineCache(REVIEWS_PREDICATE_CACHE,      1000, 60),
                buildCaffeineCache(REVIEW_STATS_CACHE,           1000, 60),
                buildCaffeineCache(RATING_DISTRIBUTION_CACHE,    1000, 60),
                buildCaffeineCache(REVIEW_TRENDS_CACHE,           500, 60),
                buildCaffeineCache(TOP_RATED_PRODUCTS_CACHE,      500, 60),
                buildCaffeineCache(MOST_REVIEWED_PRODUCTS_CACHE,  500, 60),
                buildCaffeineCache(USER_REVIEWS_CACHE,           1000, 60),
                buildCaffeineCache(REVIEW_LISTS_CACHE,           1000, 60),
                buildCaffeineCache(ADMIN_REVIEWS_CACHE,           500, 60),

                buildCaffeineCache(WISHLIST_CACHE,           1000, 60),
                buildCaffeineCache(WISHLIST_PAGINATED_CACHE, 1000, 60),
                buildCaffeineCache(WISHLIST_SUMMARY_CACHE,   1000, 60),
                buildCaffeineCache(WISHLIST_CHECK_CACHE,     2000, 60),

                // --- LONG_LIVED (2-3 hours) - Rarely changing data ---
                buildCaffeineCache(PRODUCTS_DISCOUNTED_CACHE,     300, 30),
                buildCaffeineCache(PRODUCTS_FEATURED_CACHE,       200, 120),
                buildCaffeineCache(PRODUCTS_NEW_CACHE,            200, 60),
                buildCaffeineCache(PRODUCTS_BESTSELLER_CACHE,     200, 60),
                buildCaffeineCache(PRODUCTS_TOP_RATED_CACHE,      200, 60),
                buildCaffeineCache(PRODUCTS_TRENDING_CACHE,       200, 30),

                buildCaffeineCache(CATEGORIES_STATS_CACHE,  200, 180),

                buildCaffeineCache(WISHLIST_DROPS_CACHE,      500, 30),
                buildCaffeineCache(WISHLIST_ANALYTICS_CACHE,  500, 60),

                buildCaffeineCache(DASHBOARD_CACHE, 10, 10),

                // --- Missing caches used by @Cacheable annotations ---
                buildCaffeineCache("admin-analytics",     50, 10),
                buildCaffeineCache("faqs",              1000, 60),
                buildCaffeineCache("searchFilters",       50, 60),
                buildCaffeineCache("settings",            50, 60),
                buildCaffeineCache("users-predicate",   1000, 60)
        ));
        return cacheManager;
    }

    private Cache buildCaffeineCache(String name, int maxSize, int ttlMinutes) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build());
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error("Cache GET error in cache '{}' for key '{}': {}", cache.getName(), key, exception.getMessage());
            }
            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.error("Cache PUT error in cache '{}' for key '{}': {}", cache.getName(), key, exception.getMessage());
            }
            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error("Cache EVICT error in cache '{}' for key '{}': {}", cache.getName(), key, exception.getMessage());
            }
            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.error("Cache CLEAR error in cache '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }


}
