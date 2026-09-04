package com.aoms.aomsbackend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "arm")
@Validated
@Slf4j
public class ArmProperties {

    @NotBlank(message = "ARM GraphQL URL must be configured (ARM_GRAPHQL_URL)")
    private String graphqlUrl;

    // Service token for background operations (attendance jobs, etc.)
    private String serviceToken;

    private long cacheTtlSeconds = 300;

    private long timeoutMs = 1500;

    private Map<String, String> roleMappings = new HashMap<>();

    public String getAccessToken() {
        return serviceToken;
    }
}
