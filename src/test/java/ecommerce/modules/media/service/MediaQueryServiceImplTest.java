package ecommerce.modules.media.service;

import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.dto.response.MediaAssetPage;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.ProductMediaItemResponse;
import ecommerce.modules.media.dto.response.StorageQuotaResponse;
import ecommerce.modules.media.dto.response.StorageUsageResponse;
import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.ProductMedia;
import ecommerce.modules.media.entity.StorageUsage;
import ecommerce.modules.media.enums.*;
import ecommerce.modules.media.exception.MediaAssetNotFoundException;
import ecommerce.modules.media.repository.MediaAssetRepository;
import ecommerce.modules.media.repository.ProductMediaRepository;
import ecommerce.modules.media.repository.StorageUsageRepository;
import ecommerce.modules.media.service.impl.MediaQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaQueryServiceImpl")
class MediaQueryServiceImplTest {

    @Mock private MediaAssetRepository    assetRepository;
    @Mock private ProductMediaRepository  productMediaRepository;
    @Mock private StorageUsageRepository  usageRepository;
    @Mock private MediaProperties         props;

    @InjectMocks
    private MediaQueryServiceImpl service;

    private UUID userId;
    private UUID assetPublicId;
    private MediaAsset asset;
    private MediaProperties.QuotaConfig quotaConfig;

    @BeforeEach
    void setUp() {
        userId        = UUID.randomUUID();
        assetPublicId = UUID.randomUUID();

        asset = MediaAsset.builder()
                .ownerId(UUID.randomUUID())
                .ownerType(MediaOwnerType.PRODUCT)
                .provider(ProviderType.R2)
                .originalFilename("banner.jpg")
                .mimeType("image/jpeg")
                .mediaType(MediaType.PRODUCT_IMAGE)
                .fileSize(2048L)
                .visibility(MediaVisibility.PUBLIC)
                .status(MediaStatus.READY)
                .uploadedBy(userId)
                .objectKey("product/uuid/original.jpg")
                .build();
        setField(asset, "publicId", assetPublicId);
        setField(asset, "id", 42L);
        setField(asset, "createdAt", Instant.now());
        setField(asset, "updatedAt", Instant.now());

        quotaConfig = new MediaProperties.QuotaConfig();
        lenient().when(props.getQuota()).thenReturn(quotaConfig);
    }

    // ── getAsset ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAsset(publicId, userId)")
    class GetAsset {

        @Test
        @DisplayName("Found — returns populated response")
        void getAsset_found_returnsResponse() {
            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));

            MediaAssetResponse response = service.getAsset(assetPublicId, userId);

            assertThat(response.publicId()).isEqualTo(assetPublicId);
            assertThat(response.mimeType()).isEqualTo("image/jpeg");
            assertThat(response.status()).isEqualTo(MediaStatus.READY);
            assertThat(response.fileSize()).isEqualTo(2048L);
        }

        @Test
        @DisplayName("Not found — throws MediaAssetNotFoundException")
        void getAsset_notFound_throwsException() {
            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAsset(assetPublicId, userId))
                    .isInstanceOf(MediaAssetNotFoundException.class);
        }
    }

    // ── getProductMedia ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProductMedia(productId)")
    class GetProductMedia {

        @Test
        @DisplayName("Returns items ordered by sort_order, primary flag propagated")
        void getProductMedia_returnsOrderedItems() {
            UUID productId = UUID.randomUUID();
            ProductMedia pm = ProductMedia.builder()
                    .productId(productId)
                    .mediaAsset(asset)
                    .sortOrder(0)
                    .isPrimary(true)
                    .altText("front view")
                    .build();

            when(productMediaRepository.findByProductIdOrderBySortOrderAsc(productId))
                    .thenReturn(List.of(pm));

            List<ProductMediaItemResponse> result = service.getProductMedia(productId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isPrimary()).isTrue();
            assertThat(result.get(0).altText()).isEqualTo("front view");
            assertThat(result.get(0).mediaAsset().publicId()).isEqualTo(assetPublicId);
        }

        @Test
        @DisplayName("No media — returns empty list")
        void getProductMedia_noMedia_returnsEmpty() {
            UUID productId = UUID.randomUUID();
            when(productMediaRepository.findByProductIdOrderBySortOrderAsc(productId))
                    .thenReturn(List.of());

            assertThat(service.getProductMedia(productId)).isEmpty();
        }
    }

    // ── getStorageQuota ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStorageQuota(userId)")
    class GetStorageQuota {

        @Test
        @DisplayName("User has existing usage — percent computed correctly")
        void getStorageQuota_withUsage_percentCorrect() {
            long quota = 1_000_000L;
            quotaConfig.setDefaultSellerQuotaBytes(quota);

            StorageUsage usage = StorageUsage.builder()
                    .storageBytes(250_000L).objectCount(5L).build();
            when(usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.of(usage));

            StorageQuotaResponse response = service.getStorageQuota(userId);

            assertThat(response.storageBytes()).isEqualTo(250_000L);
            assertThat(response.quotaBytes()).isEqualTo(quota);
            assertThat(response.usagePercent()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("No usage record — returns zeroes")
        void getStorageQuota_noUsage_returnsZero() {
            when(usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.empty());

            StorageQuotaResponse response = service.getStorageQuota(userId);

            assertThat(response.storageBytes()).isZero();
            assertThat(response.usagePercent()).isZero();
        }
    }

    // ── getMyAssets ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyAssets(userId, pageable)")
    class GetMyAssets {

        @Test
        @DisplayName("Returns paginated asset list mapped to responses")
        void getMyAssets_withAssets_returnsMappedPage() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<MediaAsset> page = new PageImpl<>(List.of(asset), pageable, 1);

            when(assetRepository.findByUploadedByAndIsActiveTrue(userId, pageable)).thenReturn(page);

            MediaAssetPage result = service.getMyAssets(userId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getCurrentPage()).isZero();
            assertThat(result.isHasNextPage()).isFalse();
            assertThat(result.getContent().get(0).publicId()).isEqualTo(assetPublicId);
        }
    }

    // ── adminListAssets ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminListAssets(status, pageable)")
    class AdminListAssets {

        @Test
        @DisplayName("With status filter — delegates to findByStatus")
        void adminListAssets_withStatus_delegatesToStatusQuery() {
            PageRequest pageable = PageRequest.of(0, 20);
            Page<MediaAsset> page = new PageImpl<>(List.of(asset));

            when(assetRepository.findByStatus(MediaStatus.READY, pageable)).thenReturn(page);

            MediaAssetPage result = service.adminListAssets(MediaStatus.READY, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("No status filter — returns all assets")
        void adminListAssets_noStatus_returnsAll() {
            PageRequest pageable = PageRequest.of(0, 20);
            Page<MediaAsset> page = new PageImpl<>(List.of(asset));

            when(assetRepository.findAll(pageable)).thenReturn(page);

            MediaAssetPage result = service.adminListAssets(null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ── getStorageUsage ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStorageUsage(userId)")
    class GetStorageUsage {

        @Test
        @DisplayName("Existing usage — mapped with quota and percent")
        void getStorageUsage_withUsage_computesPercent() {
            long quota = 2_000L;
            quotaConfig.setDefaultSellerQuotaBytes(quota);

            StorageUsage usage = StorageUsage.builder()
                    .ownerId(userId).ownerType(MediaOwnerType.USER)
                    .storageBytes(1_000L).objectCount(3L).bandwidthBytes(500L)
                    .build();
            setField(usage, "updatedAt", Instant.now());

            when(usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.of(usage));

            StorageUsageResponse response = service.getStorageUsage(userId);

            assertThat(response.getStorageBytes()).isEqualTo(1_000L);
            assertThat(response.getQuotaBytes()).isEqualTo(quota);
            assertThat(response.getPercentUsed()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("No usage record — defaults to zeroes")
        void getStorageUsage_noUsage_returnsZeroRecord() {
            when(usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.empty());

            StorageUsageResponse response = service.getStorageUsage(userId);

            assertThat(response.getStorageBytes()).isZero();
            assertThat(response.getObjectCount()).isZero();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not set field " + fieldName, e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
            current = current.getSuperclass();
        }
        throw new RuntimeException("Field not found: " + name);
    }
}
