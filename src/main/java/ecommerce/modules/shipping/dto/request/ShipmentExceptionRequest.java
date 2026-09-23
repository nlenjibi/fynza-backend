package ecommerce.modules.shipping.dto.request;

import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipmentExceptionRequest {
    @NotNull
    private ExceptionType type;
    @NotNull
    private ExceptionSeverity severity;
    private String description;
}
