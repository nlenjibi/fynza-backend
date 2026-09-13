package ecommerce.modules.category.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CreateAttributeDefinitionRequest;
import ecommerce.modules.category.dto.request.CreateAttributeOptionRequest;
import ecommerce.modules.category.dto.request.UpdateAttributeDefinitionRequest;
import ecommerce.modules.category.dto.response.AttributeDefinitionResponse;
import ecommerce.modules.category.dto.response.AttributeOptionResponse;
import ecommerce.modules.category.entity.AttributeDefinition;
import ecommerce.modules.category.entity.AttributeOption;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.AttributeDefinitionRepository;
import ecommerce.modules.category.repository.AttributeOptionRepository;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.service.CategoryAttributeService;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    private final CategoryRepository            categoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AttributeOptionRepository     attributeOptionRepository;
    private final CategoryMapper                mapper;
    private final AuditLogService               auditLogService;

    @Override
    @Transactional
    public AttributeDefinitionResponse createAttribute(UUID categoryPublicId, CreateAttributeDefinitionRequest request) {
        Category category = resolveCategory(categoryPublicId);
        if (attributeDefinitionRepository.existsByCategoryIdAndCode(category.getId(), request.getCode())) {
            throw new BadRequestException("Attribute code '" + request.getCode() + "' already exists for this category");
        }

        AttributeDefinition def = AttributeDefinition.builder()
                .categoryId(category.getId())
                .name(request.getName())
                .code(request.getCode())
                .dataType(request.getDataType())
                .unit(request.getUnit())
                .required(request.getRequired() != null ? request.getRequired() : false)
                .filterable(request.getFilterable() != null ? request.getFilterable() : false)
                .searchable(request.getSearchable() != null ? request.getSearchable() : false)
                .variantDefining(request.getVariantDefining() != null ? request.getVariantDefining() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        AttributeDefinition saved = attributeDefinitionRepository.save(def);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_ATTRIBUTE_CREATED)
                .entityType("CATEGORY_ATTRIBUTE")
                .entityPublicId(saved.getPublicId())
                .actorPublicId(null)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Attribute created: code={}, categoryId={}", saved.getCode(), category.getId());
        return mapper.toAttributeDefinitionResponse(saved, List.of());
    }

    @Override
    @Transactional
    public AttributeDefinitionResponse updateAttribute(UUID attributePublicId, UpdateAttributeDefinitionRequest request) {
        AttributeDefinition def = resolveAttribute(attributePublicId);

        if (request.getName() != null)            def.setName(request.getName());
        if (request.getUnit() != null)            def.setUnit(request.getUnit());
        if (request.getRequired() != null)        def.setRequired(request.getRequired());
        if (request.getFilterable() != null)      def.setFilterable(request.getFilterable());
        if (request.getSearchable() != null)      def.setSearchable(request.getSearchable());
        if (request.getVariantDefining() != null) def.setVariantDefining(request.getVariantDefining());
        if (request.getSortOrder() != null)       def.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null)        def.setIsActive(request.getIsActive());

        attributeDefinitionRepository.save(def);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_ATTRIBUTE_UPDATED)
                .entityType("CATEGORY_ATTRIBUTE")
                .entityPublicId(def.getPublicId())
                .actorPublicId(null)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        var options = attributeOptionRepository
                .findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(def.getId())
                .stream().map(mapper::toAttributeOptionResponse).toList();
        return mapper.toAttributeDefinitionResponse(def, options);
    }

    @Override
    @Transactional
    public void deleteAttribute(UUID attributePublicId) {
        AttributeDefinition def = resolveAttribute(attributePublicId);
        def.setIsActive(false);
        attributeDefinitionRepository.save(def);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_ATTRIBUTE_DELETED)
                .entityType("CATEGORY_ATTRIBUTE")
                .entityPublicId(def.getPublicId())
                .actorPublicId(null)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Attribute soft-deleted: publicId={}", attributePublicId);
    }

    @Override
    public List<AttributeDefinitionResponse> getAttributesByCategory(UUID categoryPublicId) {
        Category category = resolveCategory(categoryPublicId);
        return attributeDefinitionRepository
                .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(category.getId())
                .stream()
                .map(def -> {
                    var options = attributeOptionRepository
                            .findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(def.getId())
                            .stream().map(mapper::toAttributeOptionResponse).toList();
                    return mapper.toAttributeDefinitionResponse(def, options);
                })
                .toList();
    }

    @Override
    @Transactional
    public AttributeOptionResponse addOption(UUID attributePublicId, CreateAttributeOptionRequest request) {
        AttributeDefinition def = resolveAttribute(attributePublicId);

        AttributeOption option = AttributeOption.builder()
                .attributeDefinitionId(def.getId())
                .value(request.getValue())
                .label(request.getLabel() != null ? request.getLabel() : request.getValue())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        attributeOptionRepository.save(option);
        log.info("Attribute option added: value={}, attributeId={}", option.getValue(), def.getId());
        return mapper.toAttributeOptionResponse(option);
    }

    @Override
    @Transactional
    public void deleteOption(UUID optionPublicId) {
        AttributeOption option = attributeOptionRepository.findByPublicId(optionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute option not found: " + optionPublicId));
        option.setIsActive(false);
        attributeOptionRepository.save(option);
        log.info("Attribute option soft-deleted: publicId={}", optionPublicId);
    }

    @Override
    public List<AttributeOptionResponse> getOptions(UUID attributePublicId) {
        AttributeDefinition def = resolveAttribute(attributePublicId);
        return attributeOptionRepository
                .findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(def.getId())
                .stream().map(mapper::toAttributeOptionResponse).toList();
    }

    private Category resolveCategory(UUID publicId) {
        return categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CategoryNotFoundException(publicId));
    }

    private AttributeDefinition resolveAttribute(UUID publicId) {
        return attributeDefinitionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found: " + publicId));
    }
}
