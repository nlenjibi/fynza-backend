package com.aoms.aomsbackend.common.exception;

import com.aoms.aomsbackend.common.responses.ErrorResponse;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.OptimisticLockException;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNKNOWN_VALUE = "unknown";

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.<Void>builder()
                        .success(false)
                        .error(Map.of("code.md", ex.getCode(), "message", ex.getMessage()))
                        .statusCode(409)
                        .build());
    }

    @ExceptionHandler(AomsException.class)
    public ResponseEntity<ErrorResponse> handleAomsException(AomsException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}] on '{}': {}", ex.getClass().getSimpleName(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.builder().code(ex.getCode()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = Stream.concat(
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> {
                            assert e.getDefaultMessage() != null;
                            return Map.entry(e.getField(), e.getDefaultMessage());
                        }),
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(e -> {
                            assert e.getDefaultMessage() != null;
                            return Map.entry(e.getObjectName(), e.getDefaultMessage());
                        })
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (existing, replacement) -> existing));

        log.warn("Validation failure: {}", errors);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().message("Malformed or unreadable request body.").build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied in MVC layer on '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .message("You do not have permission to access this resource.")
                        .build());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(Exception ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure on '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .code("CONCURRENT_MODIFICATION")
                        .message("The record was modified by another request. Please retry.")
                        .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoStaticResource(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message("Resource not found.")
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getParameter().getParameterName() != null
                ? ex.getParameter().getParameterName()
                : UNKNOWN_VALUE;
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();

        log.warn("Method argument type mismatch for parameter '{}': received '{}', expected {}",
                paramName, value != null ? value.toString() : "null", requiredType != null ? requiredType.getSimpleName() : UNKNOWN_VALUE);

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .message("Invalid parameter '" + paramName + "': expected " +
                                (requiredType != null ? requiredType.getSimpleName() : "unknown type") +
                                ", received '" + (value != null ? value.toString() : "null") + "'")
                        .errors(Map.of(
                                "parameter", paramName,
                                "received", value != null ? value.toString() : "null",
                                "expected", requiredType != null ? requiredType.getSimpleName() : UNKNOWN_VALUE
                        ))
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on '{}': {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message("An unexpected error occurred. Please contact support if this persists.")
                        .build());
    }
}
