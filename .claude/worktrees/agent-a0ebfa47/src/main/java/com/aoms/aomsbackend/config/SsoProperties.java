package com.aoms.aomsbackend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@ConfigurationProperties(prefix = "sso")
@Validated
public class SsoProperties {

    @NotBlank(message = "SSO endpoint must be configured (SSO_ENDPOINT)")
    private String endpoint;
}
