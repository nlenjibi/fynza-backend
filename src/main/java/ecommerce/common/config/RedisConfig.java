package ecommerce.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
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

import static ecommerce.common.config.CacheConfig.*;

/**
 * Redis-backed cache configuration, active on non-test profiles.
 *
 * <h3>Serialization security (OWASP A08)</h3>
 * Uses {@link BasicPolymorphicTypeValidator} with an explicit package allowlist
 * instead of the unsafe {@code LaissezFaireSubTypeValidator}, which would permit
 * deserialization of arbitrary classes — a known RCE vector (CVE-2017-7525 family).
 *
 * <h3>Connection factory</h3>
 * Spring Boot auto-configures a {@code LettuceConnectionFactory} from
 * {@code spring.data.redis.*} properties (SSL, sentinel, cluster, pooling
 * all handled). We inject the auto-configured factory rather than creating one.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }

    // ── Redis ObjectMapper ────────────────────────────────────────────────────
    // Separate from the HTTP ObjectMapper. Default typing writes @class metadata
    // so cached values round-trip to their original type (not LinkedHashMap).
    // The validator restricts deserialization to known ecommerce packages only.

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

        Map<String, RedisCacheConfiguration> perCache = Map.ofEntries(
                // Auth / Principals
                entry(base, USER_PRINCIPALS_CACHE,      Duration.ofMinutes(5)),
                // Products
                entry(base, PRODUCTS_CACHE,             Duration.ofMinutes(5)),
                entry(base, PRODUCTS_PAGE_CACHE,        Duration.ofMinutes(5)),
                entry(base, PRODUCTS_SEARCH_CACHE,      Duration.ofMinutes(5)),
                entry(base, PRODUCTS_FEATURED_CACHE,    Duration.ofMinutes(30)),
                entry(base, PRODUCTS_BESTSELLER_CACHE,  Duration.ofMinutes(30)),
                entry(base, PRODUCTS_TRENDING_CACHE,    Duration.ofMinutes(10)),
                // Categories
                entry(base, CATEGORIES_CACHE,           Duration.ofHours(1)),
                entry(base, CATEGORIES_LIST_CACHE,      Duration.ofHours(1)),
                // Wishlists
                entry(base, WISHLIST_CACHE,             Duration.ofMinutes(30)),
                entry(base, WISHLIST_SUMMARY_CACHE,     Duration.ofMinutes(30)),
                // Orders
                entry(base, ORDER_CACHE,                Duration.ofMinutes(15)),
                entry(base, ORDER_STATS_CACHE,          Duration.ofMinutes(15)),
                // Reviews
                entry(base, REVIEWS_CACHE,              Duration.ofMinutes(15)),
                entry(base, REVIEW_STATS_CACHE,         Duration.ofMinutes(15)),
                // Users
                entry(base, USERS_CACHE,                Duration.ofMinutes(15)),
                // Security / Tokens
                entry(base, "tokenBlacklist",           Duration.ofHours(24)),
                entry(base, "stockReservations",        Duration.ofMinutes(15)),
                // Misc
                entry(base, "faqs",                     Duration.ofHours(1)),
                entry(base, "settings",                 Duration.ofHours(1)),
                entry(base, DASHBOARD_CACHE,            Duration.ofMinutes(5))
        );

        log.info("Redis CacheManager configured with {} per-cache TTLs", perCache.size());
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(15)))
                .withInitialCacheConfigurations(perCache)
                .build();
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
