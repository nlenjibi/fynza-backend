package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.ArmProfile;
import com.aoms.aomsbackend.auth.service.impl.ArmIntegrationServiceImpl;
import com.aoms.aomsbackend.config.ArmProperties;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmIntegrationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ArmIntegrationServiceImpl armIntegrationService;

    @BeforeEach
    void setUp() {
        ArmProperties armProperties = new ArmProperties();
        armProperties.setGraphqlUrl("https://arm.test.example.com/graphql");
        armProperties.setCacheTtlSeconds(300);
        armProperties.setTimeoutMs(1500);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        armIntegrationService = new ArmIntegrationServiceImpl(
                armProperties, redisTemplate, new JsonMapper()
        );
    }

    @Test
    void fetchProfile_cacheHit_returnsProfileWithoutArmCall() {
        ArmProfile cached = ArmProfile.builder()
            .department("Engineering")
            .position("Senior Engineer")
            .build();
        String cachedJson = new JsonMapper().writeValueAsString(cached);
        when(valueOperations.get("arm:profile:user-123")).thenReturn(cachedJson);

        ArmProfile result = armIntegrationService.fetchProfile("user-123", "test-token");

        assertThat(result.getDepartment()).isEqualTo("Engineering");
        assertThat(result.getPosition()).isEqualTo("Senior Engineer");
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }

    @Test
    void fetchProfile_cacheMiss_armUnavailable_returnsEmptyProfile() {
        when(valueOperations.get(anyString())).thenReturn(null);

        ArmProfile result = armIntegrationService.fetchProfile("user-unreachable", "test-token");

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void fetchProfile_cacheMiss_corruptCacheEntry_returnsEmptyProfile() {
        when(valueOperations.get("arm:profile:user-corrupt")).thenReturn("NOT_VALID_JSON{{{");

        ArmProfile result = armIntegrationService.fetchProfile("user-corrupt", "test-token");

        assertThat(result.isEmpty()).isTrue();
    }
}