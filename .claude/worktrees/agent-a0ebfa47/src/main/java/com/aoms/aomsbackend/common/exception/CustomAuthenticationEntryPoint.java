package com.aoms.aomsbackend.common.exception;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Throwable cause = authException.getCause() != null ? authException.getCause() : authException;

        if (cause instanceof AuthenticationServiceException) {
            log.error("Auth service error on '{}': {}", request.getRequestURI(), cause.getMessage(), cause);
        } else {
            log.info("Auth failure [{}] on '{}'", cause.getClass().getSimpleName(), request.getRequestURI());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(response.getOutputStream(), Map.of("message", resolveMessage(authException, cause)));
    }

    private String resolveMessage(AuthenticationException ex, Throwable cause) {
        if (cause instanceof AuthenticationServiceException) {
            return "Unable to process authentication at this time. Please try again later.";
        }
        if (ex instanceof InsufficientAuthenticationException) {
            return "Please sign in to access this resource.";
        }
        return "Authentication failed. Please sign in again to continue.";
    }
}
