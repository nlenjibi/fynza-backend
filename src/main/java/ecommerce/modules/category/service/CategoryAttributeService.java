package ecommerce.modules.category.service;

import ecommerce.modules.category.dto.request.CreateAttributeDefinitionRequest;
import ecommerce.modules.category.dto.request.CreateAttributeOptionRequest;
import ecommerce.modules.category.dto.request.UpdateAttributeDefinitionRequest;
import ecommerce.modules.category.dto.response.AttributeDefinitionResponse;
import ecommerce.modules.category.dto.response.AttributeOptionResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryAttributeService {

    AttributeDefinitionResponse createAttribute(UUID categoryPublicId, CreateAttributeDefinitionRequest request);

    AttributeDefinitionResponse updateAttribute(UUID attributePublicId, UpdateAttributeDefinitionRequest request);

    void deleteAttribute(UUID attributePublicId);

    List<AttributeDefinitionResponse> getAttributesByCategory(UUID categoryPublicId);

    AttributeOptionResponse addOption(UUID attributePublicId, CreateAttributeOptionRequest request);

    void deleteOption(UUID optionPublicId);

    List<AttributeOptionResponse> getOptions(UUID attributePublicId);
}
