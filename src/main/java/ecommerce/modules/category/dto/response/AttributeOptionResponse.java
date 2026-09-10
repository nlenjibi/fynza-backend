package ecommerce.modules.category.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AttributeOptionResponse {
    UUID publicId;
    String value;
    String label;
    Integer sortOrder;
    Boolean isActive;
}
