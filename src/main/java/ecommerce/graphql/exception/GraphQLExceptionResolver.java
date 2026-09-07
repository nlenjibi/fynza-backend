package ecommerce.graphql.exception;

import ecommerce.common.exception.FynzaException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps Fynza domain and framework exceptions to GraphQL errors, mirroring
 * {@code GlobalExceptionHandler} on the REST transport.
 *
 * <p>{@code @RestControllerAdvice} does not apply to GraphQL data-fetchers, so this
 * resolver preserves the same status/code/message semantics on the GraphQL {@code errors}
 * channel. Clients keying on {@code extensions.status} and {@code extensions.code} get
 * identical information regardless of whether they use REST or GraphQL.
 */
@Slf4j
@Component
public class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        // Domain exceptions — covers every FynzaException subclass
        if (ex instanceof FynzaException fynzaEx) {
            return fromFynzaException(fynzaEx, env);
        }
        if (ex instanceof AccessDeniedException accessEx) {
            return fromAccessDenied(accessEx, env);
        }
        if (ex instanceof ConstraintViolationException cve) {
            return fromConstraintViolation(cve, env);
        }
        if (ex instanceof MethodArgumentNotValidException manv) {
            return fromMethodArgumentNotValid(manv, env);
        }
        // Fall through — framework logs INTERNAL_ERROR automatically.
        return null;
    }

    // ── Exception mappers ──────────────────────────────────────────────────────

    private GraphQLError fromFynzaException(FynzaException ex, DataFetchingEnvironment env) {
        log.warn("GraphQL domain exception [{}] on '{}': {}",
                ex.getClass().getSimpleName(), env.getField().getName(), ex.getMessage());

        Map<String, Object> ext = baseExtensions(ex.getStatus());
        if (ex.getCode() != null) ext.put("code", ex.getCode());

        return GraphqlErrorBuilder.newError(env)
                .errorType(classificationFor(ex.getStatus()))
                .message(ex.getMessage())
                .extensions(ext)
                .build();
    }

    private GraphQLError fromAccessDenied(AccessDeniedException ex, DataFetchingEnvironment env) {
        log.warn("GraphQL access denied on '{}': {}", env.getField().getName(), ex.getMessage());

        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.FORBIDDEN)
                .message("You do not have permission to access this resource.")
                .extensions(baseExtensions(HttpStatus.FORBIDDEN))
                .build();
    }

    private GraphQLError fromConstraintViolation(ConstraintViolationException ex, DataFetchingEnvironment env) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing));
        log.warn("GraphQL constraint violation on '{}': {}", env.getField().getName(), fieldErrors);
        return validationError(env, fieldErrors);
    }

    private GraphQLError fromMethodArgumentNotValid(MethodArgumentNotValidException ex, DataFetchingEnvironment env) {
        Map<String, String> fieldErrors = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> Map.entry(e.getField(),
                                        e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid")),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(e -> Map.entry(e.getObjectName(),
                                        e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (existing, replacement) -> existing));
        log.warn("GraphQL validation failure on '{}': {}", env.getField().getName(), fieldErrors);
        return validationError(env, fieldErrors);
    }

    private GraphQLError validationError(DataFetchingEnvironment env, Map<String, String> fieldErrors) {
        Map<String, Object> ext = baseExtensions(HttpStatus.BAD_REQUEST);
        ext.put("errors", fieldErrors);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message("Validation failed.")
                .extensions(ext)
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Map<String, Object> baseExtensions(HttpStatus status) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("status", status.value());
        ext.put("reason", status.name());
        return ext;
    }

    /**
     * Buckets an HTTP status into the nearest Spring {@link ErrorType}. The exact status is
     * always preserved in {@code extensions.status} and {@code extensions.reason}, so 409/422
     * still surface losslessly even though they bucket to {@code BAD_REQUEST}.
     */
    private ErrorType classificationFor(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> ErrorType.UNAUTHORIZED;
            case FORBIDDEN    -> ErrorType.FORBIDDEN;
            case NOT_FOUND    -> ErrorType.NOT_FOUND;
            default           -> status.is5xxServerError() ? ErrorType.INTERNAL_ERROR : ErrorType.BAD_REQUEST;
        };
    }
}
