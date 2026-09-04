package com.aoms.aomsbackend.support;

import com.aoms.aomsbackend.config.JwtTokenProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class TestSecurityMockBase {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
}

