package com.aoms.aomsbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableScheduling
@Data
@Component
@ConfigurationProperties(prefix = "sync")
@Validated
public class SchedulingConfig {
    private  String noShowCron;
}
