package ecommerce.modules.category.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.category.dto.CategoryCreateRequest;
import ecommerce.modules.category.dto.CategoryResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryResponse testCategoryResponse;
    private CategoryCreateRequest testCreateRequest;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        
        testCategory = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices and accessories")
                .isActive(true)
                .build();

        testCategoryResponse = CategoryResponse.builder()
                .id(categoryId)
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices and accessories")
                .isActive(true)
                .build();

        testCreateRequest = CategoryCreateRequest.builder()
                .name("Electronics")
                .description("Electronic devices and accessories")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should return all categories")
        void findAll_ReturnsAllCategories() {
            List<Category> categories = Arrays.asList(testCategory);
            when(categoryRepository.findAll()).thenReturn(categories);
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            List<CategoryResponse> result = categoryService.findAll();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Electronics", result.get(0).getName());
            verify(categoryRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void findAll_WhenNoCategories_ReturnsEmptyList() {
            when(categoryRepository.findAll()).thenReturn(List.of());

            List<CategoryResponse> result = categoryService.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should return category when found")
        void findById_WhenCategoryExists_ReturnsCategory() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            CategoryResponse result = categoryService.findById(categoryId);

            assertNotNull(result);
            assertEquals("Electronics", result.getName());
            assertEquals("electronics", result.getSlug());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category not found")
        void findById_WhenCategoryNotFound_ThrowsException() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> categoryService.findById(categoryId));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should create category successfully")
        void create_WhenValidRequest_CreatesCategory() {
            when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
            when(categoryMapper.toEntityFromRequest(any(CategoryCreateRequest.class))).thenReturn(testCategory);
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            CategoryResponse result = categoryService.create(testCreateRequest);

            assertNotNull(result);
            assertEquals("Electronics", result.getName());
            verify(categoryRepository, times(1)).save(any(Category.class));
        }

        @Test
        @DisplayName("Should generate unique slug when slug exists")
        void create_WhenSlugExists_GeneratesUniqueSlug() {
            testCategory.setSlug("electronics-1");
            
            when(categoryRepository.existsBySlug("electronics")).thenReturn(true);
            when(categoryRepository.existsBySlug("electronics-1")).thenReturn(false);
            when(categoryMapper.toEntityFromRequest(any(CategoryCreateRequest.class))).thenReturn(testCategory);
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            CategoryResponse result = categoryService.create(testCreateRequest);

            assertNotNull(result);
            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should update category successfully")
        void update_WhenValidRequest_UpdatesCategory() {
            CategoryCreateRequest updateRequest = CategoryCreateRequest.builder()
                    .name("Updated Electronics")
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsBySlug("updated-electronics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            CategoryResponse result = categoryService.update(categoryId, updateRequest);

            assertNotNull(result);
            verify(categoryRepository, times(1)).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent category")
        void update_WhenCategoryNotFound_ThrowsException() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.update(categoryId, testCreateRequest));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should delete category successfully")
        void delete_WhenCategoryExists_DeletesCategory() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            doNothing().when(categoryRepository).delete(testCategory);

            categoryService.delete(categoryId);

            verify(categoryRepository, times(1)).delete(testCategory);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent category")
        void delete_WhenCategoryNotFound_ThrowsException() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.delete(categoryId));
        }
    }

    @Nested
    @DisplayName("findTree")
    class FindTreeTests {

        @Test
        @DisplayName("Should return category tree")
        void findTree_ReturnsCategoryTree() {
            Category parentCategory = Category.builder()
                    .id(categoryId)
                    .name("Parent")
                    .slug("parent")
                    .isActive(true)
                    .build();
            
            when(categoryRepository.findRootCategories()).thenReturn(List.of(parentCategory));
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            List<CategoryResponse> result = categoryService.findTree();

            assertNotNull(result);
            verify(categoryRepository, times(1)).findRootCategories();
        }
    }

    @Nested
    @DisplayName("findActiveCategories")
    class FindActiveCategoriesTests {

        @Test
        @DisplayName("Should return only active categories")
        void findActiveCategories_ReturnsActiveCategories() {
            when(categoryRepository.findByIsActive(true)).thenReturn(List.of(testCategory));
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            List<CategoryResponse> result = categoryService.findActiveCategories();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.get(0).getIsActive());
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update category status to inactive")
        void updateStatus_ShouldUpdateStatus() {
            testCategory.setIsActive(false);
            
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(categoryMapper.toSimpleResponse(any(Category.class))).thenReturn(testCategoryResponse);

            CategoryResponse result = categoryService.updateStatus(categoryId, false);

            assertNotNull(result);
            verify(categoryRepository, times(1)).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("getCategoryStats")
    class GetCategoryStatsTests {

        @Test
        @DisplayName("Should return category statistics")
        void getCategoryStats_ReturnsStats() {
            when(categoryRepository.count()).thenReturn(10L);
            when(categoryRepository.countActiveCategories()).thenReturn(8L);
            when(categoryRepository.countSubcategories()).thenReturn(5L);
            when(categoryRepository.countParentCategories()).thenReturn(5L);

            var result = categoryService.getCategoryStats();

            assertNotNull(result);
            assertEquals(10L, result.get("totalCategories"));
            assertEquals(8L, result.get("activeCategories"));
            assertEquals(5L, result.get("subcategories"));
            assertEquals(5L, result.get("parentCategories"));
        }
    }
}
