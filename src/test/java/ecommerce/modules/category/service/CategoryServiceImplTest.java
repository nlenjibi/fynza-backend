package ecommerce.modules.category.service;

import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CategorySearchRequest;
import ecommerce.modules.category.dto.request.CreateCategoryRequest;
import ecommerce.modules.category.dto.request.MoveCategoryRequest;
import ecommerce.modules.category.dto.request.UpdateCategoryRequest;
import ecommerce.modules.category.dto.response.CategoryDetailResponse;
import ecommerce.modules.category.dto.response.CategorySummaryResponse;
import ecommerce.modules.category.dto.response.CategoryTreeResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.entity.CategorySummaryView;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.AttributeDefinitionRepository;
import ecommerce.modules.category.repository.AttributeOptionRepository;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.repository.CategorySummaryViewRepository;
import ecommerce.modules.category.service.impl.CategoryServiceImpl;
import ecommerce.modules.category.validation.CategoryHierarchyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Tests")
class CategoryServiceImplTest {

    @Mock private CategoryRepository            categoryRepository;
    @Mock private CategorySummaryViewRepository summaryViewRepository;
    @Mock private AttributeDefinitionRepository attributeDefinitionRepository;
    @Mock private AttributeOptionRepository     attributeOptionRepository;
    @Mock private CategoryMapper                mapper;
    @Mock private CategoryHierarchyValidator    hierarchyValidator;
    @Mock private AuditLogService               auditLogService;

    @InjectMocks
    private CategoryServiceImpl service;

    private UUID actorId;
    private UUID categoryPublicId;
    private Category category;

    @BeforeEach
    void setUp() {
        actorId          = UUID.randomUUID();
        categoryPublicId = UUID.randomUUID();

        category = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .status(CategoryStatus.DRAFT)
                .visibility(CategoryVisibility.PUBLIC)
                .sortOrder(0)
                .isActive(true)
                .build();
        setId(category, 1L);
        setPublicId(category, categoryPublicId);

        when(attributeDefinitionRepository.findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(anyLong()))
                .thenReturn(List.of());
        when(summaryViewRepository.findByParentIdOrderBySortOrderAsc(anyLong()))
                .thenReturn(List.of());
        when(mapper.toDetailResponse(any(), any(), any()))
                .thenReturn(CategoryDetailResponse.builder()
                        .publicId(categoryPublicId).name("Electronics").slug("electronics")
                        .status(CategoryStatus.DRAFT).attributes(List.of()).children(List.of()).build());
    }

    @Nested
    @DisplayName("createCategory(request, actorUserId)")
    class CreateCategory {

        @Test
        @DisplayName("Happy path — slug generated from name, audit logged with saved publicId")
        void createCategory_happyPath_slugGeneratedAndAuditLogged() {
            CreateCategoryRequest request = CreateCategoryRequest.builder().name("Electronics").build();

            when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                setPublicId(c, categoryPublicId);
                return c;
            });

            CategoryDetailResponse result = service.createCategory(request, actorId);

            assertThat(result).isNotNull();
            assertThat(result.getPublicId()).isEqualTo(categoryPublicId);

            ArgumentCaptor<Category> savedCaptor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getSlug()).isEqualTo("electronics");
            assertThat(savedCaptor.getValue().getStatus()).isEqualTo(CategoryStatus.DRAFT);
            assertThat(savedCaptor.getValue().getIsActive()).isTrue();

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEntityPublicId()).isEqualTo(categoryPublicId);
            assertThat(auditCaptor.getValue().getActorPublicId()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("Slug uniqueness — null taxonomyId uses existsBySlug and appends counter on conflict")
        void createCategory_nullTaxonomyId_usesExistsBySlugForUniqueness() {
            CreateCategoryRequest request = CreateCategoryRequest.builder().name("Electronics").build();

            when(categoryRepository.existsBySlug("electronics")).thenReturn(true);
            when(categoryRepository.existsBySlug("electronics-2")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                setPublicId(c, categoryPublicId);
                return c;
            });

            service.createCategory(request, actorId);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getSlug()).isEqualTo("electronics-2");
            verify(categoryRepository, never()).existsBySlugAndTaxonomyId(anyString(), anyLong());
        }

        @Test
        @DisplayName("Slug uniqueness — non-null taxonomyId uses existsBySlugAndTaxonomyId")
        void createCategory_withTaxonomyId_usesExistsBySlugAndTaxonomyId() {
            long taxonomyId = 10L;
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Electronics").taxonomyId(taxonomyId).build();

            when(categoryRepository.existsBySlugAndTaxonomyId("electronics", taxonomyId)).thenReturn(true);
            when(categoryRepository.existsBySlugAndTaxonomyId("electronics-2", taxonomyId)).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                setPublicId(c, categoryPublicId);
                return c;
            });

            service.createCategory(request, actorId);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getSlug()).isEqualTo("electronics-2");
            assertThat(captor.getValue().getTaxonomyId()).isEqualTo(taxonomyId);
            verify(categoryRepository, never()).existsBySlug(anyString());
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when parentCategoryPublicId does not exist")
        void createCategory_parentNotFound_throwsCategoryNotFoundException() {
            UUID parentId = UUID.randomUUID();
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Phones").parentCategoryPublicId(parentId).build();

            when(categoryRepository.existsBySlug("phones")).thenReturn(false);
            when(categoryRepository.findByPublicId(parentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createCategory(request, actorId))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(parentId.toString());

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Parent found — hierarchy depth validated and parent set on entity")
        void createCategory_withValidParent_setsParentAndValidatesDepth() {
            UUID parentPubId = UUID.randomUUID();
            Category parent = Category.builder().name("Tech").slug("tech").build();
            setId(parent, 2L);
            setPublicId(parent, parentPubId);

            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Phones").parentCategoryPublicId(parentPubId).build();

            when(categoryRepository.existsBySlug("phones")).thenReturn(false);
            when(categoryRepository.findByPublicId(parentPubId)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                setPublicId(c, categoryPublicId);
                return c;
            });

            service.createCategory(request, actorId);

            verify(hierarchyValidator).validateDepth(parent);
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getParentCategory()).isEqualTo(parent);
        }
    }

    @Nested
    @DisplayName("updateCategory(categoryPublicId, request, actorUserId)")
    class UpdateCategory {

        @Test
        @DisplayName("Name change regenerates slug and saves updated entity")
        void updateCategory_nameChanged_regeneratesSlug() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.existsBySlug("new-electronics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            service.updateCategory(categoryPublicId, UpdateCategoryRequest.builder()
                    .name("New Electronics").build(), actorId);

            assertThat(category.getName()).isEqualTo("New Electronics");
            assertThat(category.getSlug()).isEqualTo("new-electronics");
            verify(categoryRepository).save(category);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Name change with null taxonomyId — slug conflict uses existsBySlug")
        void updateCategory_nameChangedNullTaxonomy_usesExistsBySlug() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.existsBySlug("gadgets")).thenReturn(true);
            when(categoryRepository.existsBySlug("gadgets-2")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            service.updateCategory(categoryPublicId, UpdateCategoryRequest.builder()
                    .name("Gadgets").build(), actorId);

            assertThat(category.getSlug()).isEqualTo("gadgets-2");
            verify(categoryRepository, never()).existsBySlugAndTaxonomyId(anyString(), anyLong());
        }

        @Test
        @DisplayName("All-null request — name and slug unchanged")
        void updateCategory_nullFields_notApplied() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any())).thenReturn(category);

            service.updateCategory(categoryPublicId, UpdateCategoryRequest.builder().build(), actorId);

            assertThat(category.getName()).isEqualTo("Electronics");
            assertThat(category.getSlug()).isEqualTo("electronics");
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when category does not exist")
        void updateCategory_notFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCategory(categoryPublicId,
                    UpdateCategoryRequest.builder().build(), actorId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("moveCategory(categoryPublicId, request, actorUserId)")
    class MoveCategory {

        @Test
        @DisplayName("Circular hierarchy throws — validateMove propagates exception")
        void moveCategory_circularHierarchy_throwsException() {
            UUID newParentId = UUID.randomUUID();
            Category newParent = Category.builder().name("Child").slug("child").build();
            setPublicId(newParent, newParentId);

            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.findByPublicId(newParentId)).thenReturn(Optional.of(newParent));
            doThrow(new ecommerce.modules.category.exception.CategoryCircularHierarchyException("Electronics"))
                    .when(hierarchyValidator).validateMove(category, newParent);

            assertThatThrownBy(() -> service.moveCategory(categoryPublicId,
                    MoveCategoryRequest.builder().newParentPublicId(newParentId).build(), actorId))
                    .isInstanceOf(ecommerce.modules.category.exception.CategoryCircularHierarchyException.class);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Max depth exceeded throws — validateMove propagates exception")
        void moveCategory_maxDepthExceeded_throwsException() {
            UUID newParentId = UUID.randomUUID();
            Category deepParent = Category.builder().name("Deep").slug("deep").build();
            setPublicId(deepParent, newParentId);

            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.findByPublicId(newParentId)).thenReturn(Optional.of(deepParent));
            doThrow(new ecommerce.modules.category.exception.CategoryMaxDepthExceededException(5))
                    .when(hierarchyValidator).validateMove(category, deepParent);

            assertThatThrownBy(() -> service.moveCategory(categoryPublicId,
                    MoveCategoryRequest.builder().newParentPublicId(newParentId).build(), actorId))
                    .isInstanceOf(ecommerce.modules.category.exception.CategoryMaxDepthExceededException.class);
        }

        @Test
        @DisplayName("Happy path — new parent set, category saved, audit logged")
        void moveCategory_happyPath_parentUpdatedAndAuditLogged() {
            UUID newParentId = UUID.randomUUID();
            Category newParent = Category.builder().name("Tech").slug("tech").build();
            setPublicId(newParent, newParentId);

            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.findByPublicId(newParentId)).thenReturn(Optional.of(newParent));
            when(categoryRepository.save(category)).thenReturn(category);

            service.moveCategory(categoryPublicId,
                    MoveCategoryRequest.builder().newParentPublicId(newParentId).build(), actorId);

            assertThat(category.getParentCategory()).isEqualTo(newParent);
            verify(hierarchyValidator).validateMove(category, newParent);
            verify(categoryRepository).save(category);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Null newParentPublicId — moves category to root")
        void moveCategory_nullNewParent_movesToRoot() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);

            service.moveCategory(categoryPublicId,
                    MoveCategoryRequest.builder().newParentPublicId(null).build(), actorId);

            assertThat(category.getParentCategory()).isNull();
            verify(hierarchyValidator).validateMove(category, null);
            verify(categoryRepository).save(category);
        }
    }

    @Nested
    @DisplayName("deleteCategory(categoryPublicId, actorUserId)")
    class DeleteCategory {

        @Test
        @DisplayName("Status set to DELETED, isActive false, audit logged")
        void deleteCategory_happyPath_softDeletesAndLogsAudit() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);

            service.deleteCategory(categoryPublicId, actorId);

            assertThat(category.getStatus()).isEqualTo(CategoryStatus.DELETED);
            assertThat(category.getIsActive()).isFalse();
            verify(categoryRepository).save(category);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getActorPublicId()).isEqualTo(actorId);
            assertThat(auditCaptor.getValue().getEntityPublicId()).isEqualTo(categoryPublicId);
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when category does not exist")
        void deleteCategory_notFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCategory(categoryPublicId, actorId))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(categoryPublicId.toString());
        }
    }

    @Nested
    @DisplayName("getCategoryByPublicId(publicId)")
    class GetCategoryByPublicId {

        @Test
        @DisplayName("Returns detail response when category exists")
        void getCategoryByPublicId_found_returnsDetailResponse() {
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));

            CategoryDetailResponse result = service.getCategoryByPublicId(categoryPublicId);

            assertThat(result).isNotNull();
            assertThat(result.getPublicId()).isEqualTo(categoryPublicId);
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when publicId does not exist")
        void getCategoryByPublicId_notFound_throwsCategoryNotFoundException() {
            UUID unknown = UUID.randomUUID();
            when(categoryRepository.findByPublicId(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCategoryByPublicId(unknown))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(unknown.toString());
        }
    }

    @Nested
    @DisplayName("getCategoryBySlug(slug)")
    class GetCategoryBySlug {

        @Test
        @DisplayName("Returns detail response when slug exists")
        void getCategoryBySlug_found_returnsDetailResponse() {
            when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(category));

            CategoryDetailResponse result = service.getCategoryBySlug("electronics");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when slug does not exist")
        void getCategoryBySlug_notFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCategoryBySlug("unknown-slug"))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining("unknown-slug");
        }
    }

    @Nested
    @DisplayName("getRootCategories(taxonomyId)")
    class GetRootCategories {

        @Test
        @DisplayName("With taxonomyId — delegates to findByTaxonomyIdAndParentIdIsNull")
        void getRootCategories_withTaxonomyId_usesFilteredQuery() {
            CategorySummaryView view = mock(CategorySummaryView.class);
            when(summaryViewRepository.findByTaxonomyIdAndParentIdIsNullOrderBySortOrderAsc(5L))
                    .thenReturn(List.of(view));
            CategorySummaryResponse resp = CategorySummaryResponse.builder().name("Root").build();
            when(mapper.toSummaryResponse(view)).thenReturn(resp);

            List<CategorySummaryResponse> result = service.getRootCategories(5L);

            assertThat(result).hasSize(1);
            verify(summaryViewRepository).findByTaxonomyIdAndParentIdIsNullOrderBySortOrderAsc(5L);
            verify(summaryViewRepository, never()).findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();
        }

        @Test
        @DisplayName("Without taxonomyId (null) — delegates to findByParentIdIsNullAndIsActiveTrue")
        void getRootCategories_withoutTaxonomyId_usesActiveFlatQuery() {
            CategorySummaryView view = mock(CategorySummaryView.class);
            when(summaryViewRepository.findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(view));
            CategorySummaryResponse resp = CategorySummaryResponse.builder().name("Root").build();
            when(mapper.toSummaryResponse(view)).thenReturn(resp);

            List<CategorySummaryResponse> result = service.getRootCategories(null);

            assertThat(result).hasSize(1);
            verify(summaryViewRepository).findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();
            verify(summaryViewRepository, never()).findByTaxonomyIdAndParentIdIsNullOrderBySortOrderAsc(anyLong());
        }
    }

    @Nested
    @DisplayName("getCategoryTree(taxonomyId)")
    class GetCategoryTree {

        @Test
        @DisplayName("With taxonomyId — uses findByTaxonomyIdAndParentCategoryIsNull")
        void getCategoryTree_withTaxonomyId_usesFilteredQuery() {
            Category root = Category.builder().name("Tech").slug("tech").build();
            setId(root, 10L);
            setPublicId(root, UUID.randomUUID());

            when(categoryRepository.findByTaxonomyIdAndParentCategoryIsNullOrderBySortOrderAsc(3L))
                    .thenReturn(List.of(root));
            when(categoryRepository.findByParentCategory_IdOrderBySortOrderAsc(10L)).thenReturn(List.of());
            CategoryTreeResponse treeResp = CategoryTreeResponse.builder()
                    .name("Tech").slug("tech").children(List.of()).build();
            when(mapper.toTreeResponse(eq(root), any())).thenReturn(treeResp);

            List<CategoryTreeResponse> result = service.getCategoryTree(3L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Tech");
            verify(categoryRepository).findByTaxonomyIdAndParentCategoryIsNullOrderBySortOrderAsc(3L);
            verify(categoryRepository, never()).findByParentCategoryIsNullOrderBySortOrderAsc();
        }

        @Test
        @DisplayName("Without taxonomyId (null) — uses findByParentCategoryIsNull")
        void getCategoryTree_withoutTaxonomyId_usesAllRootsQuery() {
            Category root = Category.builder().name("All").slug("all").build();
            setId(root, 11L);
            setPublicId(root, UUID.randomUUID());

            when(categoryRepository.findByParentCategoryIsNullOrderBySortOrderAsc()).thenReturn(List.of(root));
            when(categoryRepository.findByParentCategory_IdOrderBySortOrderAsc(11L)).thenReturn(List.of());
            CategoryTreeResponse treeResp = CategoryTreeResponse.builder()
                    .name("All").slug("all").children(List.of()).build();
            when(mapper.toTreeResponse(eq(root), any())).thenReturn(treeResp);

            List<CategoryTreeResponse> result = service.getCategoryTree(null);

            assertThat(result).hasSize(1);
            verify(categoryRepository).findByParentCategoryIsNullOrderBySortOrderAsc();
            verify(categoryRepository, never()).findByTaxonomyIdAndParentCategoryIsNullOrderBySortOrderAsc(anyLong());
        }
    }

    @Nested
    @DisplayName("searchCategories(request, pageable)")
    class SearchCategories {

        @Test
        @DisplayName("Delegates to summaryViewRepository with spec and maps results")
        @SuppressWarnings("unchecked")
        void searchCategories_delegatesToSpec_returnsMappedPage() {
            CategorySearchRequest request = CategorySearchRequest.builder()
                    .query("electronics").isActive(true).build();
            Pageable pageable = PageRequest.of(0, 10);

            CategorySummaryView view = mock(CategorySummaryView.class);
            Page<CategorySummaryView> viewPage = new PageImpl<>(List.of(view));
            when(summaryViewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(viewPage);
            CategorySummaryResponse resp = CategorySummaryResponse.builder().name("Electronics").build();
            when(mapper.toSummaryResponse(view)).thenReturn(resp);

            Page<CategorySummaryResponse> result = service.searchCategories(request, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Electronics");
            verify(summaryViewRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    private static void setId(Category c, Long id) {
        try {
            java.lang.reflect.Field field = Category.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(c, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void setPublicId(Category c, UUID publicId) {
        try {
            java.lang.reflect.Field field = Category.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(c, publicId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
