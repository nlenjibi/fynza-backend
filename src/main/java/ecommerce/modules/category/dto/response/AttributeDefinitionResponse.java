package ecommerce.modules.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.modules.category.enums.AttributeDataType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttributeDefinitionResponse {
    UUID publicId;
    String name;
    String code;
    AttributeDataType dataType;
    String unit;
    Boolean required;
    Boolean filterable;
    Boolean searchable;
    Boolean variantDefining;
    Integer sortOrder;
    Boolean isActive;
    List<AttributeOptionResponse> options;
}
