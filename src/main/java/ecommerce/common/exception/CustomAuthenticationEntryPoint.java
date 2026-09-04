package ecommerce.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String GENERIC_ERROR = "Authentication failed. Please sign in again to continue.";

    private static final Map<Class<? extends Throwable>, String> EXCEPTION_MESSAGES = Map.ofEntries(
            // JWT Token Issues
            Map.entry(ExpiredJwtException.class, "Your session has expired. Please sign in again."),
            Map.entry(MalformedJwtException.class, "Invalid authentication token format. Please sign in again."),
            Map.entry(SignatureException.class, "Authentication token verification failed. Please sign in again."),
            Map.entry(UnsupportedJwtException.class, "Unsupported authentication token. Please update your app or sign in again."),
            Map.entry(JwtException.class, "Invalid authentication token. Please sign in again."),
            Map.entry(IllegalArgumentException.class, "No authentication token provided. Please sign in to access this resource."),

            // Credentials Issues
            Map.entry(BadCredentialsException.class, "Invalid email or password. Please check your credentials and try again."),
            Map.entry(UsernameNotFoundException.class, "No account found with this email. Please sign up first."),

            // Account Status Issues
            Map.entry(AccountExpiredException.class, "Your account has expired. Please contact support to reactivate it."),
            Map.entry(LockedException.class, "Your account has been locked. Please contact support for assistance."),
            Map.entry(DisabledException.class, "Your account has been disabled. Please verify your email or contact support."),
            Map.entry(CredentialsExpiredException.class, "Your password has expired. Please reset your password to continue."),

            // Authentication Process Issues
            Map.entry(AuthenticationCredentialsNotFoundException.class, "No authentication credentials found. Please sign in again."),
            Map.entry(InsufficientAuthenticationException.class, "Please sign in to access this resource."),
            Map.entry(AuthenticationServiceException.class, "Unable to process authentication at this time. Please try again later.")
    );

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Throwable root = authException.getCause() != null ? authException.getCause() : authException;

        // Log appropriate level based on exception type
        if (isServerError(root)) {
            log.error("Authentication service error on '{}': {}", request.getRequestURI(), root.getMessage(), root);
        } else if (isSecurityViolation(root)) {
            log.warn("Security violation [{}] on '{}': {}", root.getClass().getSimpleName(), request.getRequestURI(), root.getMessage());
        } else {
            log.info("Authentication failure [{}] on '{}': {}", root.getClass().getSimpleName(), request.getRequestURI(), root.getMessage());
        }

        String message = resolveUserFriendlyMessage(authException, root);
        writeJsonResponse(response, message);
    }

    private boolean isServerError(Throwable root) {
        return root instanceof AuthenticationServiceException;
    }

    private boolean isSecurityViolation(Throwable root) {
        return root instanceof JwtException ||
                root instanceof SignatureException ||
                root instanceof MalformedJwtException;
    }

    private String resolveUserFriendlyMessage(AuthenticationException authException, Throwable root) {
        // Try to get specific message for the root cause
        String message = EXCEPTION_MESSAGES.get(root.getClass());
        if (message != null) {
            return message;
        }

        // Try to get message for the auth exception
        message = EXCEPTION_MESSAGES.get(authException.getClass());
        if (message != null) {
            return message;
        }

        // For InsufficientAuthenticationException, provide context about the requested resource
        if (authException instanceof InsufficientAuthenticationException) {
            return "Authentication required. Please sign in to access this feature.";
        }

        // Log unexpected exception type for debugging
        log.debug("Unexpected authentication exception type: {}", authException.getClass().getName());

        return GENERIC_ERROR;
    }

    private void writeJsonResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = Map.of(
                "message", message
        );

        MAPPER.writeValue(response.getOutputStream(), errorResponse);
    }
}