package ecommerce.modules.product.service;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.enums.ProductType;
import ecommerce.common.enums.ProductVisibility;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.product.dto.request.CreateProductRequest;
import ecommerce.modules.product.dto.request.UpdateProductRequest;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.exception.ProductNotFoundException;
import ecommerce.modules.product.mapper.ProductMapper;
import ecommerce.modules.product.policy.ProductOwnershipPolicy;
import ecommerce.modules.product.repository.ProductCategoryRepository;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.repository.ProductStatusHistoryRepository;
import ecommerce.modules.product.service.impl.ProductServiceImpl;
import ecommerce.modules.product.validation.ProductStatusTransitionValidator;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

    @Mock private ProductRepository              productRepository;
    @Mock private ProductCategoryRepository      categoryRepository;
    @Mock private ProductStatusHistoryRepository statusHistoryRepository;
    @Mock private StoreRepository                storeRepository;
    @Mock private ProductOwnershipPolicy         ownershipPolicy;
    @Mock private StoreOwnershipPolicy           storeOwnershipPolicy;
    @Mock private ProductNumberService           numberService;
    @Mock private ProductSlugService             slugService;
    @Mock private ProductStatusTransitionValidator transitionValidator;
    @Mock private ProductMapper                  mapper;
    @Mock private AuditLogService                auditLogService;

    @InjectMocks
    private ProductServiceImpl service;

    private UUID actorUserId;
    private UUID storePublicId;
    private UUID productId;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        actorUserId  = UUID.randomUUID();
        storePublicId = UUID.randomUUID();
        productId    = UUID.randomUUID();

        store = Store.builder()
                .sellerId(1L)
                .storeName("Test Store")
                .build();
        setField(store, "id", 10L);
        setField(store, "publicId", storePublicId);

        product = Product.builder()
                .productNumber("PROD-000001")
                .storeId(10L)
                .sellerId(1L)
                .name("Wireless Headphones")
                .slug("wireless-headphones")
                .status(ProductStatus.DRAFT)
                .visibility(ProductVisibility.PRIVATE)
                .productType(ProductType.SIMPLE)
                .isActive(true)
                .build();
        setField(product, "id", productId);

        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(statusHistoryRepository.save(any())).thenReturn(null);
        when(mapper.toResponse(any(Product.class))).thenReturn(
                ProductResponse.builder().id(productId).productNumber("PROD-000001").build());
    }

    // ── createProduct() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProduct()")
    class CreateProduct {

        @Test
        @DisplayName("Happy path — creates product with DRAFT status")
        void createProduct_happyPath() {
            when(storeOwnershipPolicy.assertOwns(storePublicId, actorUserId)).thenReturn(store);
            when(slugService.generateSlug("Wireless Headphones")).thenReturn("wireless-headphones");
            when(numberService.nextProductNumber()).thenReturn("PROD-000001");

            CreateProductRequest request = new CreateProductRequest();
            request.setName("Wireless Headphones");

            ProductResponse result = service.createProduct(actorUserId, storePublicId, request);

            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Assigns primary category when provided")
        void createProduct_withCategory_assignsCategory() {
            UUID categoryId = UUID.randomUUID();
            when(storeOwnershipPolicy.assertOwns(storePublicId, actorUserId)).thenReturn(store);
            when(slugService.generateSlug(any())).thenReturn("test-product");
            when(numberService.nextProductNumber()).thenReturn("PROD-000002");

            CreateProductRequest request = new CreateProductRequest();
            request.setName("Test Product");
            request.setPrimaryCategoryId(categoryId);

            service.createProduct(actorUserId, storePublicId, request);

            verify(categoryRepository).save(any());
        }
    }

    // ── updateProduct() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProduct()")
    class UpdateProduct {

        @Test
        @DisplayName("Updates name and re-generates slug")
        void updateProduct_updateName_updatesSlug() {
            when(ownershipPolicy.assertOwns(productId, actorUserId)).thenReturn(product);
            when(slugService.ensureUnique("new name", productId)).thenReturn("new-name");

            UpdateProductRequest request = new UpdateProductRequest();
            request.setName("new name");

            service.updateProduct(actorUserId, productId, request);

            assertThat(product.getName()).isEqualTo("new name");
            assertThat(product.getSlug()).isEqualTo("new-name");
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Null name preserves existing value")
        void updateProduct_nullName_preservesExisting() {
            when(ownershipPolicy.assertOwns(productId, actorUserId)).thenReturn(product);

            UpdateProductRequest request = new UpdateProductRequest();

            service.updateProduct(actorUserId, productId, request);

            assertThat(product.getName()).isEqualTo("Wireless Headphones");
            verify(slugService, never()).ensureUnique(any(), any());
        }
    }

    // ── findById() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Returns response when product exists")
        void findById_found_returnsResponse() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            ProductResponse result = service.findById(productId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Throws ProductNotFoundException when not found")
        void findById_notFound_throws() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ── findByStore() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByStore()")
    class FindByStore {

        @Test
        @DisplayName("Throws StoreNotFoundException when store does not exist")
        void findByStore_storeNotFound_throws() {
            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByStore(storePublicId, null))
                    .isInstanceOf(StoreNotFoundException.class);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
