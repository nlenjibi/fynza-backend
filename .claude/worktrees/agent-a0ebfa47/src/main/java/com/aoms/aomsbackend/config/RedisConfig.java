package com.aoms.aomsbackend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_USER_ROLES = "authz_user_roles";
    public static final String CACHE_ARM_PROFILES = "arm_profiles";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ArmProperties armProperties) {
        RedisCacheConfiguration userRolesConfig = baseConfig()
                .entryTtl(Duration.ofMinutes(30));

        RedisCacheConfiguration armProfileConfig = baseConfig()
                .entryTtl(Duration.ofSeconds(armProperties.getCacheTtlSeconds()));

        RedisCacheConfiguration defaultConfig = baseConfig()
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CACHE_USER_ROLES, userRolesConfig)
                .withCacheConfiguration(CACHE_ARM_PROFILES, armProfileConfig)
                .build();
    }

    private RedisCacheConfiguration baseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues();
    }
}
