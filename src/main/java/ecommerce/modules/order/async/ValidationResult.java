package ecommerce.modules.order.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private String stage;
    private String message;
    private Map<String, Object> details;
    private List<String> warnings;

    public static ValidationResult success(String stage) {
        return ValidationResult.builder()
                .valid(true)
                .stage(stage)
                .message("Validation passed")
                .build();
    }

    public static ValidationResult success(String stage, String message) {
        return ValidationResult.builder()
                .valid(true)
                .stage(stage)
                .message(message)
                .build();
    }

    public static ValidationResult failure(String stage, String message) {
        return ValidationResult.builder()
                .valid(false)
                .stage(stage)
                .message(message)
                .build();
    }

    public static ValidationResult failure(String stage, String message, Map<String, Object> details) {
        return ValidationResult.builder()
                .valid(false)
                .stage(stage)
                .message(message)
                .details(details)
                .build();
    }

    public static ValidationResult neutral(String stage, String message) {
        return ValidationResult.builder()
                .valid(true)
                .stage(stage)
                .message(message)
                .build();
    }
}
