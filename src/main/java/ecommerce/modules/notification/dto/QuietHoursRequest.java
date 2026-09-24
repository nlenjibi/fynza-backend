package ecommerce.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuietHoursRequest {

    @NotBlank
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "startTime must be HH:mm")
    private String startTime;

    @NotBlank
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "endTime must be HH:mm")
    private String endTime;

    @NotBlank
    private String timezone;
}
