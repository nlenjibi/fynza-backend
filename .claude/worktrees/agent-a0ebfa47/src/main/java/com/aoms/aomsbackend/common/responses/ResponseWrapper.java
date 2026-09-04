package com.aoms.aomsbackend.common.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private Boolean success;
    private String message;
    private T data;
    private Object errors;
    private Map<String, String> error;
    private Integer statusCode;

    public static <T> ResponseWrapper<T> success(T data) {
        return ResponseWrapper.<T>builder()
            .timestamp(LocalDateTime.now())
            .success(true)
            .data(data)
            .statusCode(200)
            .build();
    }

    public static <T> ResponseWrapper<T> success(String message, T data) {
        return ResponseWrapper.<T>builder()
            .timestamp(LocalDateTime.now())
            .success(true)
            .message(message)
            .data(data)
            .statusCode(200)
            .build();
    }

    public static <T> ResponseWrapper<T> error(String message) {
        return ResponseWrapper.<T>builder()
            .timestamp(LocalDateTime.now())
            .success(false)
            .message(message)
            .statusCode(400)
            .build();
    }

    public static <T> ResponseWrapper<T> unauthorized() {
        return ResponseWrapper.<T>builder()
            .success(false)
            .error(Map.of("code.md", "UNAUTHORIZED", "message", "Session invalid or expired."))
            .statusCode(401)
            .build();
    }

    public static <T> ResponseWrapper<T> internalError() {
        return ResponseWrapper.<T>builder()
            .success(false)
            .error(Map.of("code.md", "INTERNAL_ERROR", "message", "An unexpected error occurred."))
            .statusCode(500)
            .build();
    }
}
