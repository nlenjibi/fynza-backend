package ecommerce.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    private static final ExceptionInfo FALLBACK = new ExceptionInfo(
            "UNAUTHORIZED", "Authentication failed. Please sign in again to continue.");

    private record ExceptionInfo(String code, String message) {}

    /**
     * Maps exception types to their (code, message) pair.
     * More-specific JWT subclasses are listed before {@link JwtException} so that
     * {@link #resolve} finds them first when walking up the class hierarchy.
     */
    private static final Map<Class<? extends Throwable>, ExceptionInfo> EXCEPTION_DETAILS = Map.ofEntries(
            // JWT — specific subtypes before the base catch-all
            Map.entry(ExpiredJwtException.class,    new ExceptionInfo("TOKEN_EXPIRED",   "Your session has expired. Please sign in again.")),
            Map.entry(SignatureException.class,      new ExceptionInfo("INVALID_TOKEN",   "Authentication token verification failed. Please sign in again.")),
            Map.entry(MalformedJwtException.class,  new ExceptionInfo("INVALID_TOKEN",   "Invalid authentication token format. Please sign in again.")),
            Map.entry(UnsupportedJwtException.class,new ExceptionInfo("INVALID_TOKEN",   "Unsupported authentication token. Please update your app or sign in again.")),
            Map.entry(JwtException.class,           new ExceptionInfo("INVALID_TOKEN",   "Invalid authentication token. Please sign in again.")),

            // Credentials
            Map.entry(BadCredentialsException.class,  new ExceptionInfo("INVALID_CREDENTIALS", "Invalid email or password. Please check your credentials and try again.")),
            Map.entry(UsernameNotFoundException.class, new ExceptionInfo("USER_NOT_FOUND",      "No account found with this email. Please sign up first.")),

            // Account status
            Map.entry(AccountExpiredException.class,     new ExceptionInfo("ACCOUNT_EXPIRED",  "Your account has expired. Please contact support to reactivate it.")),
            Map.entry(LockedException.class,             new ExceptionInfo("ACCOUNT_LOCKED",   "Your account has been locked. Please contact support for assistance.")),
            Map.entry(DisabledException.class,           new ExceptionInfo("ACCOUNT_DISABLED", "Your account has been disabled. Please verify your email or contact support.")),
            Map.entry(CredentialsExpiredException.class, new ExceptionInfo("PASSWORD_EXPIRED", "Your password has expired. Please reset your password to continue.")),

            // Auth process
            Map.entry(AuthenticationCredentialsNotFoundException.class, new ExceptionInfo("NO_CREDENTIALS",     "No authentication credentials found. Please sign in again.")),
            Map.entry(InsufficientAuthenticationException.class,        new ExceptionInfo("UNAUTHORIZED",        "Please sign in to access this resource.")),
            Map.entry(AuthenticationServiceException.class,             new ExceptionInfo("SERVICE_UNAVAILABLE", "Unable to process authentication at this time. Please try again later."))
    );

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Throwable root = authException.getCause() != null ? authException.getCause() : authException;

        if (root instanceof AuthenticationServiceException) {
            log.error("Authentication service error on '{}': {}", request.getRequestURI(), root.getMessage(), root);
        } else if (root instanceof JwtException) {
            log.warn("Security violation [{}] on '{}': {}", root.getClass().getSimpleName(), request.getRequestURI(), root.getMessage());
        } else {
            log.info("Authentication failure [{}] on '{}': {}", root.getClass().getSimpleName(), request.getRequestURI(), root.getMessage());
        }

        ExceptionInfo info = resolve(root);
        if (info == null && root != authException) {
            info = resolve(authException);
        }
        if (info == null) {
            log.debug("Unexpected authentication exception type: {}", authException.getClass().getName());
            info = FALLBACK;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                .code(info.code())
                .message(info.message())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build());
    }

    /**
     * Walks the class hierarchy of {@code t} upward until a match is found in
     * {@link #EXCEPTION_DETAILS}, allowing base-class entries (e.g. {@link JwtException})
     * to act as catch-alls for any unlisted subclass.
     */
    private static ExceptionInfo resolve(Throwable t) {
        Class<?> cls = t.getClass();
        while (cls != null && cls != Throwable.class) {
            ExceptionInfo info = EXCEPTION_DETAILS.get(cls);
            if (info != null) return info;
            cls = cls.getSuperclass();
        }
        return null;
    }
}
