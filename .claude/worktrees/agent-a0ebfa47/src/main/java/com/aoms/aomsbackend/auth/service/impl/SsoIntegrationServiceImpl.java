package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.dto.SSOUserProfile;
import com.aoms.aomsbackend.auth.service.SsoIntegrationService;
import com.aoms.aomsbackend.common.exception.TokenVerificationException;
import com.aoms.aomsbackend.config.SsoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SsoIntegrationServiceImpl implements SsoIntegrationService {

    private final SsoProperties ssoProperties;

    @Override
    public SSOUserProfile fetchProfile(String appToken) {
        try {
            SSOUserProfile profile = RestClient.builder()
                .baseUrl(ssoProperties.getEndpoint())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appToken)
                .build()
                .get()
                .uri("/user/profile/info")
                .retrieve()
                .body(SSOUserProfile.class);

            if (profile == null) {
                log.warn("SSO returned null profile");
                throw new TokenVerificationException();
            }

            log.debug("SSO profile fetched for userId={}", profile.getUserId());
            return profile;

        } catch (TokenVerificationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("SSO profile fetch failed: {}", ex.getMessage());
            throw new TokenVerificationException();
        } catch (Exception ex) {
            log.warn("Unexpected error fetching SSO profile: {}", ex.getMessage());
            throw new TokenVerificationException();
        }
    }
}
