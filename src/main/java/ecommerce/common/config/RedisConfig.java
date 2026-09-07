package ecommerce.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ecommerce.common.cache.CacheNames;
import ecommerce.common.cache.CacheProperties;
import ecommerce.common.cache.RedisCircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties({RedisProperties.class, CacheProperties.class})
public class RedisConfig {

    private final RedisProperties redisProperties;
    private final CacheProperties cacheProperties;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection: host={}, port={}",
                redisProperties.getHost(), redisProperties.getPort());
        return new LettuceConnectionFactory(
                redisProperties.getHost(),
                redisProperties.getPort());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        log.info("RedisTemplate configured");
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getRedis().getDefaultTtl())
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Products — 5 min (read-heavy, change with catalog updates)
        cacheConfigs.put(CacheNames.PRODUCTS,               defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_PAGE,          defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_SEARCH,        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_PREDICATE,     defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_FILTER,        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_CATEGORY,      defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_CATEGORY_NAME, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_PRICE_RANGE,   defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_STATUS,        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_REORDER,       defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_DISCOUNTED,    defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.PRODUCTS_FEATURED,      defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CacheNames.PRODUCTS_NEW,           defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CacheNames.PRODUCTS_BESTSELLER,    defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CacheNames.PRODUCTS_TOP_RATED,     defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CacheNames.PRODUCTS_TRENDING,      defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Categories — 1 hour (rarely change)
        cacheConfigs.put(CacheNames.CATEGORIES,        defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put(CacheNames.CATEGORIES_LIST,   defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put(CacheNames.CATEGORIES_PAGED,  defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put(CacheNames.CATEGORIES_SEARCH, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put(CacheNames.CATEGORIES_FILTER, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put(CacheNames.CATEGORIES_STATS,  defaultConfig.entryTtl(Duration.ofHours(1)));

        // Orders — 15 min
        cacheConfigs.put(CacheNames.ORDERS,           defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDER,            defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDER_EXISTS,     defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USER_ORDERS,      defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDER_STATS,      defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDER_COUNTS,     defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDERS_PREDICATE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDERS_SEARCH,    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ORDERS_FILTER,    defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Users — 15 min
        cacheConfigs.put(CacheNames.USERS,           defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USERS_PAGE,      defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USERS_SEARCH,    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USERS_ROLE,      defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USERS_ACTIVE,    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USERS_PREDICATE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USER_PRINCIPALS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.USER_PROFILE,    defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Reviews — 15 min
        cacheConfigs.put(CacheNames.REVIEWS,                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.REVIEW,                 defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.REVIEWS_PREDICATE,      defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.REVIEW_STATS,           defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.RATING_DISTRIBUTION,    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.REVIEW_TRENDS,          defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.TOP_RATED_PRODUCTS,     defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.MOST_REVIEWED_PRODUCTS, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.USER_REVIEWS,           defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.REVIEW_LISTS,           defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(CacheNames.ADMIN_REVIEWS,          defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Wishlists — 30 min
        cacheConfigs.put(CacheNames.WISHLIST,           defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CacheNames.WISHLIST_PAGINATED, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CacheNames.WISHLIST_SUMMARY,   defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CacheNames.WISHLIST_CHECK,     defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CacheNames.WISHLIST_DROPS,     defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CacheNames.WISHLIST_ANALYTICS, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Security-critical
        cacheConfigs.put(CacheNames.TOKEN_BLACKLIST,    defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put(CacheNames.STOCK_RESERVATIONS, defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Admin / misc
        cacheConfigs.put(CacheNames.ADMIN_DASHBOARD,  defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CacheNames.ADMIN_ANALYTICS,  defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CacheNames.SELLER_DASHBOARD, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CacheNames.FAQS,             defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put(CacheNames.SEARCH_FILTERS,   defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put(CacheNames.SETTINGS,         defaultConfig.entryTtl(Duration.ofMinutes(60)));

        log.info("Redis CacheManager configured with {} named caches", cacheConfigs.size());
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
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

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
