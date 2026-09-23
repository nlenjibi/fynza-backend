package ecommerce.modules.shipping.dto.request;

import lombok.Data;

@Data
public class ResolveExceptionRequest {
    private String resolutionNotes;
    private String resolvedBy;
}
