package ecommerce.common.exception;

import ecommerce.common.audit.RateLimitingAspect;
import ecommerce.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import javax.security.auth.login.AccountLockedException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ══════════════════════════════════════════════════════════════════════════
    // Domain exceptions — single handler covers all FynzaException subclasses
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(FynzaException.class)
    public ResponseEntity<ErrorResponse> handleFynzaException(FynzaException ex,
                                                               HttpServletRequest request) {
        log.warn("Domain exception [{}] on '{}': {}", ex.getClass().getSimpleName(),
                request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(ex.getStatus().value())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 400 – Validation & bad input
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = Stream.concat(
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> Map.entry(e.getField(), e.getDefaultMessage())),
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(e -> Map.entry(e.getObjectName(), e.getDefaultMessage()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (existing, replacement) -> existing));

        log.warn("Validation failure on '{}': {}", request.getRequestURI(), errors);

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .errors(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(400)
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("Constraint violation on '{}': {}", request.getRequestURI(), errors);

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .errors(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(400)
                .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body on '{}': {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("MALFORMED_REQUEST")
                .message("Request body is missing or malformed.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(400)
                .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing parameter '{}' on '{}'", ex.getParameterName(), request.getRequestURI());

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("MISSING_PARAMETER")
                .message("Required parameter '" + ex.getParameterName() + "' is missing.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(400)
                .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' must be of type %s.", ex.getName(), expected);
        log.warn("Type mismatch on '{}': {}", request.getRequestURI(), message);

        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code("TYPE_MISMATCH")
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(400)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 401 – Authentication failures (thrown from MVC layer, not filter chain)
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleBadCredentials(AuthenticationException ex,
                                                               HttpServletRequest request) {
        log.warn("Authentication failure [{}] on '{}': {}", ex.getClass().getSimpleName(),
                request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.builder()
                .code("INVALID_CREDENTIALS")
                .message("Invalid username or password.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(401)
                .build());
    }

    @ExceptionHandler({
            AccountExpiredException.class,
            LockedException.class,
            DisabledException.class,
            CredentialsExpiredException.class
    })
    public ResponseEntity<ErrorResponse> handleAccountStatus(AuthenticationException ex,
                                                              HttpServletRequest request) {
        log.warn("Account status failure [{}] on '{}': {}", ex.getClass().getSimpleName(),
                request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.builder()
                .code("ACCOUNT_STATUS_ERROR")
                .message(resolveAccountStatusMessage(ex))
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(401)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 403 – Authorization failures
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * NOTE: AccessDeniedException from the Security filter chain is handled by
     * {@link CustomAccessDeniedHandler}. This covers the MVC / @PreAuthorize path.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                             HttpServletRequest request) {
        log.warn("Access denied on '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .code("FORBIDDEN")
                .message("You do not have permission to access this resource.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(403)
                .build());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex,
                                                              HttpServletRequest request) {
        log.warn("Locked account access attempt on '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.builder()
                .code("ACCOUNT_LOCKED")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(403)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 404 – Static resource not found (avoids 500 noise in logs)
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoStaticResource(NoResourceFoundException ex,
                                                                 HttpServletRequest request) {
        log.debug("Static resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                .code("NOT_FOUND")
                .message("Resource not found.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(404)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 409 – Data conflicts
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String cause = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Data integrity violation on '{}': {}", request.getRequestURI(), cause);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .code("DATA_CONFLICT")
                .message("A data conflict occurred. The resource may already exist.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(409)
                .build());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {

        log.warn("Optimistic locking conflict on '{}': {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .code("CONCURRENT_MODIFICATION")
                .message("The resource was modified by another request. Please retry.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(409)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 429 – Rate limiting
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(RateLimitingAspect.RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitingAspect.RateLimitExceededException ex, HttpServletRequest request) {

        log.warn("Rate limit exceeded on '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ErrorResponse.builder()
                .code("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please slow down and try again later.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(429)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 500 – Catch-all (must be last)
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on '{}': {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please contact support if this persists.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .status(500)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private String resolveAccountStatusMessage(AuthenticationException ex) {
        if (ex instanceof LockedException)             return "Your account is locked. Please contact support.";
        if (ex instanceof DisabledException)           return "Your account has been disabled. Please contact support.";
        if (ex instanceof AccountExpiredException)     return "Your account has expired. Please contact support.";
        if (ex instanceof CredentialsExpiredException) return "Your password has expired. Please reset your password.";
        return "Your account is not in a valid state. Please contact support.";
    }
}
