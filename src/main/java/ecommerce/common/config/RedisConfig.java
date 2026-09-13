package ecommerce.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ecommerce.common.cache.CacheNames;
import ecommerce.common.cache.CacheProperties;
import ecommerce.common.cache.RedisCircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed cache configuration, active on non-test profiles.
 *
 * <h3>Serialization security (OWASP A08)</h3>
 * Uses {@link BasicPolymorphicTypeValidator} with an explicit package allowlist
 * instead of the unsafe {@code LaissezFaireSubTypeValidator}, which would permit
 * deserialization of arbitrary classes — a known RCE vector (CVE-2017-7525 family).
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class RedisConfig implements CachingConfigurer {

    private final CacheProperties cacheProperties;

    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }

    // ── Redis ObjectMapper ────────────────────────────────────────────────────

    private static ObjectMapper buildRedisObjectMapper() {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("ecommerce.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("org.springframework.")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    }

    private static final GenericJackson2JsonRedisSerializer REDIS_SERIALIZER =
            new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());

    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();

    // ── RedisTemplate ────────────────────────────────────────────────────────

    @Bean
    @Profile("!test")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(STRING_SERIALIZER);
        template.setValueSerializer(REDIS_SERIALIZER);
        template.setHashKeySerializer(STRING_SERIALIZER);
        template.setHashValueSerializer(REDIS_SERIALIZER);
        template.afterPropertiesSet();
        return template;
    }

    // ── CacheManager ─────────────────────────────────────────────────────────

    @Bean
    @Profile("!test")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = baseConfig();

        Duration defaultTtl = cacheProperties.getRedis().getDefaultTtl();

        Map<String, RedisCacheConfiguration> perCache = Map.ofEntries(
                // Auth / Principals
                entry(base, CacheNames.USER_PRINCIPALS,         Duration.ofMinutes(5)),
                entry(base, CacheNames.USER_PROFILE,            Duration.ofMinutes(15)),
                // Products
                entry(base, CacheNames.PRODUCTS,                Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_PAGE,           Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_SEARCH,         Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_PREDICATE,      Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_FILTER,         Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_CATEGORY,       Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_CATEGORY_NAME,  Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_PRICE_RANGE,    Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_STATUS,         Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_REORDER,        Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_DISCOUNTED,     Duration.ofMinutes(5)),
                entry(base, CacheNames.PRODUCTS_FEATURED,       Duration.ofMinutes(30)),
                entry(base, CacheNames.PRODUCTS_NEW,            Duration.ofMinutes(10)),
                entry(base, CacheNames.PRODUCTS_BESTSELLER,     Duration.ofMinutes(10)),
                entry(base, CacheNames.PRODUCTS_TOP_RATED,      Duration.ofMinutes(10)),
                entry(base, CacheNames.PRODUCTS_TRENDING,       Duration.ofMinutes(5)),
                // Categories
                entry(base, CacheNames.CATEGORIES,              Duration.ofHours(1)),
                entry(base, CacheNames.CATEGORIES_LIST,         Duration.ofHours(1)),
                entry(base, CacheNames.CATEGORIES_PAGED,        Duration.ofHours(1)),
                entry(base, CacheNames.CATEGORIES_SEARCH,       Duration.ofHours(1)),
                entry(base, CacheNames.CATEGORIES_FILTER,       Duration.ofHours(1)),
                entry(base, CacheNames.CATEGORIES_STATS,        Duration.ofHours(1)),
                // Orders
                entry(base, CacheNames.ORDERS,                  Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDER,                   Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDER_EXISTS,            Duration.ofMinutes(15)),
                entry(base, CacheNames.USER_ORDERS,             Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDER_STATS,             Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDER_COUNTS,            Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDERS_PREDICATE,        Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDERS_SEARCH,           Duration.ofMinutes(15)),
                entry(base, CacheNames.ORDERS_FILTER,           Duration.ofMinutes(15)),
                // Users
                entry(base, CacheNames.USERS,                   Duration.ofMinutes(15)),
                entry(base, CacheNames.USERS_PAGE,              Duration.ofMinutes(15)),
                entry(base, CacheNames.USERS_SEARCH,            Duration.ofMinutes(15)),
                entry(base, CacheNames.USERS_ROLE,              Duration.ofMinutes(15)),
                entry(base, CacheNames.USERS_ACTIVE,            Duration.ofMinutes(15)),
                entry(base, CacheNames.USERS_PREDICATE,         Duration.ofMinutes(15)),
                // Reviews
                entry(base, CacheNames.REVIEWS,                 Duration.ofMinutes(15)),
                entry(base, CacheNames.REVIEW,                  Duration.ofMinutes(15)),
                entry(base, CacheNames.REVIEWS_PREDICATE,       Duration.ofMinutes(15)),
                entry(base, CacheNames.REVIEW_STATS,            Duration.ofMinutes(15)),
                entry(base, CacheNames.RATING_DISTRIBUTION,     Duration.ofMinutes(15)),
                entry(base, CacheNames.REVIEW_TRENDS,           Duration.ofMinutes(15)),
                entry(base, CacheNames.TOP_RATED_PRODUCTS,      Duration.ofMinutes(15)),
                entry(base, CacheNames.MOST_REVIEWED_PRODUCTS,  Duration.ofMinutes(15)),
                entry(base, CacheNames.USER_REVIEWS,            Duration.ofMinutes(15)),
                entry(base, CacheNames.REVIEW_LISTS,            Duration.ofMinutes(15)),
                entry(base, CacheNames.ADMIN_REVIEWS,           Duration.ofMinutes(15)),
                // Wishlists
                entry(base, CacheNames.WISHLIST,                Duration.ofMinutes(30)),
                entry(base, CacheNames.WISHLIST_PAGINATED,      Duration.ofMinutes(30)),
                entry(base, CacheNames.WISHLIST_SUMMARY,        Duration.ofMinutes(30)),
                entry(base, CacheNames.WISHLIST_CHECK,          Duration.ofMinutes(30)),
                entry(base, CacheNames.WISHLIST_DROPS,          Duration.ofMinutes(30)),
                entry(base, CacheNames.WISHLIST_ANALYTICS,      Duration.ofMinutes(30)),
                // Security-critical
                entry(base, CacheNames.TOKEN_BLACKLIST,         cacheProperties.getRedis().getTokenBlacklistTtl()),
                entry(base, CacheNames.STOCK_RESERVATIONS,      Duration.ofMinutes(15)),
                // Admin / misc
                entry(base, CacheNames.ADMIN_DASHBOARD,         Duration.ofMinutes(5)),
                entry(base, CacheNames.ADMIN_ANALYTICS,         Duration.ofMinutes(10)),
                entry(base, CacheNames.SELLER_DASHBOARD,        Duration.ofMinutes(10)),
                entry(base, CacheNames.FAQS,                    Duration.ofHours(1)),
                entry(base, CacheNames.SEARCH_FILTERS,          Duration.ofHours(1)),
                entry(base, CacheNames.SETTINGS,                Duration.ofHours(1))
        );

        log.info("Redis CacheManager configured with {} per-cache TTLs (default={})", perCache.size(), defaultTtl);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(defaultTtl))
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    public RedisCircuitBreaker redisCircuitBreaker() {
        CacheProperties.CircuitBreaker cb = cacheProperties.getCircuitBreaker();
        return new RedisCircuitBreaker(
                cb.getFailureThreshold(),
                cb.getWaitDuration(),
                cb.getHalfOpenRequests());
    }

    private static Map.Entry<String, RedisCacheConfiguration> entry(
            RedisCacheConfiguration base, String name, Duration ttl) {
        return Map.entry(name, base.entryTtl(ttl));
    }

    private static RedisCacheConfiguration baseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("fynza:")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(STRING_SERIALIZER))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(REDIS_SERIALIZER))
                .disableCachingNullValues();
    }
}
