package ecommerce.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Handles all 403 Forbidden errors within the Spring Security filter chain.
 *
 * <p>Invoked when a fully authenticated user attempts to access a resource for
 * which they lack the required role or permission.
 *
 * <p>Note: Do not confuse this with {@code CustomAuthenticationEntryPoint}.
 * <ul>
 *   <li>{@code CustomAuthenticationEntryPoint} → 401 (not authenticated)</li>
 *   <li>{@code CustomAccessDeniedHandler}      → 403 (authenticated but not authorized)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String username = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName() : "anonymous";

        log.warn("Access denied [{}] for user '{}' on '{}': {}",
                accessDeniedException.getClass().getSimpleName(), username,
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .code("FORBIDDEN")
                .message("You do not have permission to access this resource.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
