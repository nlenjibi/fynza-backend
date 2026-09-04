package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.dto.ArmProfile;
import com.aoms.aomsbackend.auth.service.ArmIntegrationService;
import com.aoms.aomsbackend.config.ArmProperties;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArmIntegrationServiceImpl implements ArmIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ArmIntegrationServiceImpl.class);
    private static final String CACHE_KEY_PREFIX = "arm:profile:";

    private static final String GET_EMPLOYEE_FOR_AUTH = """
            query GetEmployeeActiveInfo($userId: ID!) {
              getEmployeeActiveInfo(user_id: $userId) {
                id
                user_id
                employee_id
                department    { id department_name }
                position      { id position_name }
                employee_type { id name }
              }
            }
            """;

    private final ArmProperties armProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    @Override
    public ArmProfile fetchProfile(String armsUserId, String authToken) {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + armsUserId);
        if (cached != null) {
            return deserialize(cached);
        }

        ArmProfile profile = fetchFromArms(armsUserId, authToken);
        cacheProfile(armsUserId, profile);
        return profile;
    }

    private ArmProfile fetchFromArms(String armsUserId, String authToken) {
        try {
            Map<String, Object> body = Map.of(
                "query", GET_EMPLOYEE_FOR_AUTH,
                "variables", Map.of("userId", armsUserId)
            );
            return parseProfile(callGraphQl(body, authToken));
        } catch (ResourceAccessException ex) {
            log.warn("ARMS timeout for armsUserId={}, applying fallback", armsUserId);
            return ArmProfile.empty();
        } catch (Exception ex) {
            log.warn("ARMS unavailable for armsUserId={}: {}", armsUserId, ex.getMessage());
            return ArmProfile.empty();
        }
    }

    private String callGraphQl(Map<String, Object> body, String authToken) {
        RestClient restClient = buildRestClient(authToken);
        return restClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);
    }

    private ArmProfile parseProfile(String responseJson) {
        try {
            JsonNode info = objectMapper.readTree(responseJson)
                .path("data").path("getEmployeeActiveInfo");

            if (info.isMissingNode() || info.isNull()) {
                log.warn("ARMS returned empty getEmployeeActiveInfo — user may not be registered");
                return ArmProfile.empty();
            }

            return ArmProfile.builder()
                .armsInfoId(text(info, "id"))
                .employeeId(text(info, "employee_id"))
                .department(text(info.path("department"), "department_name"))
                .position(text(info.path("position"), "position_name"))
                .employeeType(text(info.path("employee_type"), "name"))
                .build();
        } catch (Exception ex) {
            log.warn("Failed to parse ARMS profile response: {}", ex.getMessage());
            return ArmProfile.empty();
        }
    }

    private void cacheProfile(String armsUserId, ArmProfile profile) {
        try {
            String json = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(
                CACHE_KEY_PREFIX + armsUserId,
                json,
                Duration.ofSeconds(armProperties.getCacheTtlSeconds())
            );
        } catch (Exception ex) {
            log.warn("Failed to cache ARMS profile for armsUserId={}", armsUserId);
        }
    }

    private ArmProfile deserialize(String json) {
        try {
            return objectMapper.readValue(json, ArmProfile.class);
        } catch (Exception ex) {
            log.warn("Failed to deserialize cached ARMS profile, refetching");
            return ArmProfile.empty();
        }
    }

    private String text(JsonNode node, String field) {
        String val = node.path(field).asString(null);
        return (val == null || val.isBlank()) ? null : val;
    }

    private RestClient buildRestClient(String authToken) {
        return RestClient.builder()
            .baseUrl(armProperties.getGraphqlUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
            .build();
    }
}