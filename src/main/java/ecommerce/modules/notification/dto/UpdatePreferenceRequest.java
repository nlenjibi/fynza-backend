package ecommerce.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePreferenceRequest {

    @NotBlank
    private String  notificationType;

    @NotBlank
    private String  channel;

    private boolean enabled;
}
