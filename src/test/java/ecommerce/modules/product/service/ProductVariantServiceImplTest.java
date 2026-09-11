package ecommerce.modules.product.service;

import ecommerce.modules.product.dto.request.CreateVariantRequest;
import ecommerce.modules.product.dto.request.UpdateVariantRequest;
import ecommerce.modules.product.dto.response.ProductVariantResponse;
import ecommerce.modules.product.entity.ProductVariant;
import ecommerce.modules.product.exception.ProductNotFoundException;
import ecommerce.modules.product.mapper.ProductMapper;
import ecommerce.modules.product.policy.ProductOwnershipPolicy;
import ecommerce.modules.product.repository.ProductVariantRepository;
import ecommerce.modules.product.service.impl.ProductVariantServiceImpl;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductVariantServiceImpl Tests")
class ProductVariantServiceImplTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductOwnershipPolicy   ownershipPolicy;
    @Mock private ProductMapper            mapper;

    @InjectMocks
    private ProductVariantServiceImpl service;

    private UUID actorUserId;
    private UUID productId;
    private UUID variantId;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        actorUserId = UUID.randomUUID();
        productId   = UUID.randomUUID();
        variantId   = UUID.randomUUID();

        variant = ProductVariant.builder()
                .productId(productId)
                .sku("SKU-001")
                .variantName("Black / M")
                .size("M")
                .color("Black")
                .variantStatus("ACTIVE")
                .isActive(true)
                .build();
        setField(variant, "id", variantId);

        when(variantRepository.save(any())).thenReturn(variant);
        when(mapper.toVariantResponse(any())).thenReturn(
                ProductVariantResponse.builder().id(variantId).sku("SKU-001").productId(productId).build());
    }

    @Nested
    @DisplayName("createVariant()")
    class CreateVariant {

        @Test
        @DisplayName("Happy path — creates variant for owned product")
        void createVariant_happyPath() {
            doNothing().when(ownershipPolicy).assertOwns(productId, actorUserId);
            when(variantRepository.existsByProductIdAndSku(productId, "SKU-001")).thenReturn(false);

            CreateVariantRequest request = new CreateVariantRequest();
            request.setSku("SKU-001");
            request.setSize("M");
            request.setColor("Black");

            ProductVariantResponse result = service.createVariant(actorUserId, productId, request);

            assertThat(result).isNotNull();
            verify(variantRepository).save(any());
        }

        @Test
        @DisplayName("Throws 409 when SKU already exists for product")
        void createVariant_duplicateSku_throws409() {
            doNothing().when(ownershipPolicy).assertOwns(productId, actorUserId);
            when(variantRepository.existsByProductIdAndSku(productId, "SKU-001")).thenReturn(true);

            CreateVariantRequest request = new CreateVariantRequest();
            request.setSku("SKU-001");

            assertThatThrownBy(() -> service.createVariant(actorUserId, productId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));

            verify(variantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteVariant()")
    class DeleteVariant {

        @Test
        @DisplayName("Soft-deletes by setting isActive = false")
        void deleteVariant_setsInactive() {
            doNothing().when(ownershipPolicy).assertOwns(productId, actorUserId);
            when(variantRepository.findByIdAndProductId(variantId, productId))
                    .thenReturn(Optional.of(variant));

            service.deleteVariant(actorUserId, productId, variantId);

            assertThat(variant.getIsActive()).isFalse();
            verify(variantRepository).save(variant);
        }

        @Test
        @DisplayName("Throws ProductNotFoundException when variant not found")
        void deleteVariant_notFound_throws() {
            doNothing().when(ownershipPolicy).assertOwns(productId, actorUserId);
            when(variantRepository.findByIdAndProductId(variantId, productId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteVariant(actorUserId, productId, variantId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

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
