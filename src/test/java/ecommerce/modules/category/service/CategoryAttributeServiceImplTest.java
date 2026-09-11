package ecommerce.modules.category.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
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
import ecommerce.modules.category.enums.AttributeDataType;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.AttributeDefinitionRepository;
import ecommerce.modules.category.repository.AttributeOptionRepository;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.service.impl.CategoryAttributeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryAttributeServiceImpl Tests")
class CategoryAttributeServiceImplTest {

    @Mock private CategoryRepository            categoryRepository;
    @Mock private AttributeDefinitionRepository attributeDefinitionRepository;
    @Mock private AttributeOptionRepository     attributeOptionRepository;
    @Mock private CategoryMapper                mapper;
    @Mock private AuditLogService               auditLogService;

    @InjectMocks
    private CategoryAttributeServiceImpl service;

    private UUID categoryPublicId;
    private UUID attributePublicId;
    private Category category;
    private AttributeDefinition attributeDef;

    @BeforeEach
    void setUp() {
        categoryPublicId  = UUID.randomUUID();
        attributePublicId = UUID.randomUUID();

        category = Category.builder().name("Electronics").slug("electronics").isActive(true).build();
        setId(category, 1L);
        setPublicId(category, categoryPublicId);

        attributeDef = AttributeDefinition.builder()
                .categoryId(1L).name("Color").code("color").dataType(AttributeDataType.TEXT)
                .required(false).filterable(true).searchable(false).variantDefining(false)
                .sortOrder(0).isActive(true).build();
        setAttrPublicId(attributeDef, attributePublicId);
        setAttrId(attributeDef, 10L);

        when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
        when(attributeDefinitionRepository.findByPublicId(attributePublicId))
                .thenReturn(Optional.of(attributeDef));
        when(attributeOptionRepository.findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(anyLong()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("createAttribute(categoryPublicId, request)")
    class CreateAttribute {

        @Test
        @DisplayName("Duplicate code — throws BadRequestException before saving")
        void createAttribute_duplicateCode_throwsBadRequestException() {
            when(attributeDefinitionRepository.existsByCategoryIdAndCode(1L, "color")).thenReturn(true);

            assertThatThrownBy(() -> service.createAttribute(categoryPublicId,
                    CreateAttributeDefinitionRequest.builder()
                            .name("Color").code("color").dataType(AttributeDataType.TEXT).build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("color");

            verify(attributeDefinitionRepository, never()).save(any());
            verify(auditLogService, never()).log(any());
        }

        @Test
        @DisplayName("Happy path — saved entity publicId used in audit (not pre-save null)")
        void createAttribute_happyPath_returnsSavedPublicId() {
            UUID savedPublicId = UUID.randomUUID();
            when(attributeDefinitionRepository.existsByCategoryIdAndCode(1L, "color")).thenReturn(false);

            AttributeDefinition saved = AttributeDefinition.builder()
                    .categoryId(1L).name("Color").code("color").dataType(AttributeDataType.TEXT)
                    .required(false).filterable(false).searchable(false).variantDefining(false)
                    .sortOrder(0).isActive(true).build();
            setAttrPublicId(saved, savedPublicId);
            setAttrId(saved, 20L);

            when(attributeDefinitionRepository.save(any(AttributeDefinition.class))).thenReturn(saved);
            AttributeDefinitionResponse expectedResponse = AttributeDefinitionResponse.builder()
                    .publicId(savedPublicId).name("Color").code("color").options(List.of()).build();
            when(mapper.toAttributeDefinitionResponse(saved, List.of())).thenReturn(expectedResponse);

            AttributeDefinitionResponse result = service.createAttribute(categoryPublicId,
                    CreateAttributeDefinitionRequest.builder()
                            .name("Color").code("color").dataType(AttributeDataType.TEXT).build());

            assertThat(result).isNotNull();
            assertThat(result.getPublicId()).isEqualTo(savedPublicId);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEntityPublicId()).isEqualTo(savedPublicId);
        }

        @Test
        @DisplayName("Category not found — throws CategoryNotFoundException")
        void createAttribute_categoryNotFound_throwsCategoryNotFoundException() {
            UUID unknownCatId = UUID.randomUUID();
            when(categoryRepository.findByPublicId(unknownCatId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createAttribute(unknownCatId,
                    CreateAttributeDefinitionRequest.builder()
                            .name("Size").code("size").dataType(AttributeDataType.TEXT).build()))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateAttribute(attributePublicId, request)")
    class UpdateAttribute {

        @Test
        @DisplayName("Partial update — only non-null fields applied to entity")
        void updateAttribute_partialUpdate_onlyNonNullFieldsApplied() {
            when(attributeDefinitionRepository.save(any())).thenReturn(attributeDef);
            when(mapper.toAttributeDefinitionResponse(any(), any()))
                    .thenReturn(AttributeDefinitionResponse.builder().publicId(attributePublicId).build());

            service.updateAttribute(attributePublicId, UpdateAttributeDefinitionRequest.builder()
                    .name("Updated Color").filterable(true).build());

            assertThat(attributeDef.getName()).isEqualTo("Updated Color");
            assertThat(attributeDef.getFilterable()).isTrue();
            assertThat(attributeDef.getCode()).isEqualTo("color");
            assertThat(attributeDef.getRequired()).isFalse();
        }

        @Test
        @DisplayName("All fields provided — all applied to entity")
        void updateAttribute_allFieldsProvided_allApplied() {
            when(attributeDefinitionRepository.save(any())).thenReturn(attributeDef);
            when(mapper.toAttributeDefinitionResponse(any(), any()))
                    .thenReturn(AttributeDefinitionResponse.builder().publicId(attributePublicId).build());

            service.updateAttribute(attributePublicId, UpdateAttributeDefinitionRequest.builder()
                    .name("Material").unit("g/m2").required(true).filterable(false)
                    .searchable(true).variantDefining(true).sortOrder(5).isActive(false).build());

            assertThat(attributeDef.getName()).isEqualTo("Material");
            assertThat(attributeDef.getUnit()).isEqualTo("g/m2");
            assertThat(attributeDef.getRequired()).isTrue();
            assertThat(attributeDef.getFilterable()).isFalse();
            assertThat(attributeDef.getSearchable()).isTrue();
            assertThat(attributeDef.getVariantDefining()).isTrue();
            assertThat(attributeDef.getSortOrder()).isEqualTo(5);
            assertThat(attributeDef.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Attribute not found — throws ResourceNotFoundException")
        void updateAttribute_notFound_throwsResourceNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(attributeDefinitionRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAttribute(unknownId,
                    UpdateAttributeDefinitionRequest.builder().build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteAttribute(attributePublicId)")
    class DeleteAttribute {

        @Test
        @DisplayName("Soft-deletes attribute — isActive false, entity saved, audit logged")
        void deleteAttribute_happyPath_softDeletesAndLogsAudit() {
            when(attributeDefinitionRepository.save(any())).thenReturn(attributeDef);

            service.deleteAttribute(attributePublicId);

            assertThat(attributeDef.getIsActive()).isFalse();
            verify(attributeDefinitionRepository).save(attributeDef);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Attribute not found — throws ResourceNotFoundException, nothing saved")
        void deleteAttribute_notFound_throwsResourceNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(attributeDefinitionRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAttribute(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(attributeDefinitionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAttributesByCategory(categoryPublicId)")
    class GetAttributesByCategory {

        @Test
        @DisplayName("Delegates to repo and maps definitions with their options")
        void getAttributesByCategory_delegatesAndMapsWithOptions() {
            AttributeDefinition def1 = AttributeDefinition.builder()
                    .categoryId(1L).name("Color").code("color")
                    .dataType(AttributeDataType.TEXT).isActive(true).build();
            setAttrId(def1, 20L);
            setAttrPublicId(def1, UUID.randomUUID());

            when(attributeDefinitionRepository.findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(1L))
                    .thenReturn(List.of(def1));

            AttributeOption opt = AttributeOption.builder()
                    .attributeDefinitionId(20L).value("Red").label("Red").build();
            when(attributeOptionRepository.findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(20L))
                    .thenReturn(List.of(opt));

            AttributeOptionResponse optResp = AttributeOptionResponse.builder()
                    .value("Red").label("Red").build();
            when(mapper.toAttributeOptionResponse(opt)).thenReturn(optResp);

            AttributeDefinitionResponse defResp = AttributeDefinitionResponse.builder()
                    .name("Color").options(List.of(optResp)).build();
            when(mapper.toAttributeDefinitionResponse(def1, List.of(optResp))).thenReturn(defResp);

            List<AttributeDefinitionResponse> result = service.getAttributesByCategory(categoryPublicId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Color");
            assertThat(result.get(0).getOptions()).hasSize(1);
            assertThat(result.get(0).getOptions().get(0).getValue()).isEqualTo("Red");
        }

        @Test
        @DisplayName("Category not found — throws CategoryNotFoundException")
        void getAttributesByCategory_categoryNotFound_throwsCategoryNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(categoryRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAttributesByCategory(unknownId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addOption(attributePublicId, request)")
    class AddOption {

        @Test
        @DisplayName("Label defaults to value when label is null in request")
        void addOption_nullLabel_defaultsLabelToValue() {
            when(attributeOptionRepository.save(any(AttributeOption.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toAttributeOptionResponse(any(AttributeOption.class)))
                    .thenReturn(AttributeOptionResponse.builder().value("XL").label("XL").build());

            service.addOption(attributePublicId,
                    CreateAttributeOptionRequest.builder().value("XL").label(null).sortOrder(1).build());

            ArgumentCaptor<AttributeOption> captor = ArgumentCaptor.forClass(AttributeOption.class);
            verify(attributeOptionRepository).save(captor.capture());
            assertThat(captor.getValue().getLabel()).isEqualTo("XL");
            assertThat(captor.getValue().getValue()).isEqualTo("XL");
        }

        @Test
        @DisplayName("Label provided — label used as-is")
        void addOption_labelProvided_usesProvidedLabel() {
            when(attributeOptionRepository.save(any(AttributeOption.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toAttributeOptionResponse(any(AttributeOption.class)))
                    .thenReturn(AttributeOptionResponse.builder().value("xl").label("Extra Large").build());

            service.addOption(attributePublicId,
                    CreateAttributeOptionRequest.builder().value("xl").label("Extra Large").build());

            ArgumentCaptor<AttributeOption> captor = ArgumentCaptor.forClass(AttributeOption.class);
            verify(attributeOptionRepository).save(captor.capture());
            assertThat(captor.getValue().getLabel()).isEqualTo("Extra Large");
        }

        @Test
        @DisplayName("Attribute not found — throws ResourceNotFoundException")
        void addOption_attributeNotFound_throwsResourceNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(attributeDefinitionRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addOption(unknownId,
                    CreateAttributeOptionRequest.builder().value("Red").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteOption(optionPublicId)")
    class DeleteOption {

        @Test
        @DisplayName("Option not found — throws ResourceNotFoundException, nothing saved")
        void deleteOption_notFound_throwsResourceNotFoundException() {
            UUID unknownOptionId = UUID.randomUUID();
            when(attributeOptionRepository.findByPublicId(unknownOptionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteOption(unknownOptionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownOptionId.toString());

            verify(attributeOptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Happy path — isActive set to false and option saved")
        void deleteOption_happyPath_softDeletesOption() {
            UUID optionPublicId = UUID.randomUUID();
            AttributeOption option = AttributeOption.builder()
                    .attributeDefinitionId(10L).value("Red").label("Red").isActive(true).build();

            when(attributeOptionRepository.findByPublicId(optionPublicId)).thenReturn(Optional.of(option));
            when(attributeOptionRepository.save(option)).thenReturn(option);

            service.deleteOption(optionPublicId);

            assertThat(option.getIsActive()).isFalse();
            verify(attributeOptionRepository).save(option);
        }
    }

    private static void setId(Category c, Long id) {
        try {
            java.lang.reflect.Field f = Category.class.getDeclaredField("id");
            f.setAccessible(true); f.set(c, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void setPublicId(Category c, UUID publicId) {
        try {
            java.lang.reflect.Field f = Category.class.getDeclaredField("publicId");
            f.setAccessible(true); f.set(c, publicId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void setAttrId(AttributeDefinition def, Long id) {
        try {
            java.lang.reflect.Field f = AttributeDefinition.class.getDeclaredField("id");
            f.setAccessible(true); f.set(def, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void setAttrPublicId(AttributeDefinition def, UUID publicId) {
        try {
            java.lang.reflect.Field f = AttributeDefinition.class.getDeclaredField("publicId");
            f.setAccessible(true); f.set(def, publicId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
