package ecommerce.modules.media.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.dto.request.AttachProductMediaRequest;
import ecommerce.modules.media.dto.request.CompleteUploadRequest;
import ecommerce.modules.media.dto.request.GenerateSignedUrlRequest;
import ecommerce.modules.media.dto.request.InitiateUploadRequest;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.SignedUrlResponse;
import ecommerce.modules.media.dto.response.UploadSessionResponse;
import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.MediaUploadSession;
import ecommerce.modules.media.entity.ProductMedia;
import ecommerce.modules.media.entity.StorageUsage;
import ecommerce.modules.media.enums.*;
import ecommerce.modules.media.exception.MediaAssetNotFoundException;
import ecommerce.modules.media.exception.StorageQuotaExceededException;
import ecommerce.modules.media.exception.UploadSessionNotFoundException;
import ecommerce.modules.media.provider.*;
import ecommerce.modules.media.repository.*;
import ecommerce.modules.media.service.impl.MediaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaServiceImpl")
class MediaServiceImplTest {

    @Mock private MediaAssetRepository        assetRepository;
    @Mock private MediaUploadSessionRepository sessionRepository;
    @Mock private ProductMediaRepository      productMediaRepository;
    @Mock private StorageUsageRepository      usageRepository;
    @Mock private StorageProviderRouter       providerRouter;
    @Mock private MediaProperties             props;
    @Mock private AuditLogService             auditLogService;
    @Mock private MediaStorageProvider        storageProvider;

    @InjectMocks
    private MediaServiceImpl service;

    private UUID userId;
    private UUID assetPublicId;
    private UUID sessionPublicId;
    private MediaAsset asset;
    private MediaUploadSession uploadSession;

    private MediaProperties.UploadConfig uploadConfig;
    private MediaProperties.QuotaConfig  quotaConfig;
    private MediaProperties.ProviderConfig providerConfig;
    private MediaProperties.R2Config      r2Config;

    @BeforeEach
    void setUp() {
        userId        = UUID.randomUUID();
        assetPublicId = UUID.randomUUID();
        sessionPublicId = UUID.randomUUID();

        asset = MediaAsset.builder()
                .ownerId(UUID.randomUUID())
                .ownerType(MediaOwnerType.PRODUCT)
                .provider(ProviderType.R2)
                .originalFilename("photo.jpg")
                .mimeType("image/jpeg")
                .mediaType(MediaType.PRODUCT_IMAGE)
                .fileSize(1024L)
                .visibility(MediaVisibility.PUBLIC)
                .status(MediaStatus.UPLOADING)
                .uploadedBy(userId)
                .objectKey("product/uuid/original.jpg")
                .build();
        setField(asset, "publicId", assetPublicId);
        setField(asset, "id", 1L);
        setField(asset, "isActive", true);
        setField(asset, "createdAt", Instant.now());
        setField(asset, "updatedAt", Instant.now());

        uploadSession = MediaUploadSession.builder()
                .userId(userId)
                .mediaAsset(asset)
                .provider(ProviderType.R2)
                .objectKey("product/uuid/original.jpg")
                .filename("photo.jpg")
                .mimeType("image/jpeg")
                .expectedSize(1024L)
                .status(UploadSessionStatus.AUTHORIZED)
                .uploadUrl("https://r2.example.com/presigned")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        setField(uploadSession, "publicId", sessionPublicId);

        uploadConfig  = new MediaProperties.UploadConfig();
        quotaConfig   = new MediaProperties.QuotaConfig();
        providerConfig = new MediaProperties.ProviderConfig();
        r2Config      = new MediaProperties.R2Config();
        r2Config.setBucket("fynza-media");
        providerConfig.setR2(r2Config);
        providerConfig.setS3(new MediaProperties.S3Config());

        lenient().when(props.getUpload()).thenReturn(uploadConfig);
        lenient().when(props.getQuota()).thenReturn(quotaConfig);
        lenient().when(props.getProvider()).thenReturn(providerConfig);
    }

    // ── initiateUpload ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initiateUpload(request, userId)")
    class InitiateUpload {

        @Test
        @DisplayName("Happy path — presigned URL issued, session and asset persisted")
        void initiateUpload_happyPath_sessionCreated() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "shoe.jpg", "image/jpeg", 2048L, 800, 600,
                    MediaType.PRODUCT_IMAGE, MediaOwnerType.PRODUCT,
                    UUID.randomUUID(), MediaVisibility.PUBLIC
            );

            UploadSession providerSession = new UploadSession(
                    UUID.randomUUID(), ProviderType.R2, UploadMethod.PRESIGNED_PUT,
                    "product/uuid/original.jpg", "https://r2.example.com/presigned",
                    Map.of("Content-Type", "image/jpeg"),
                    Instant.now().plusSeconds(600)
            );

            when(usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.empty());
            when(providerRouter.resolve()).thenReturn(storageProvider);
            when(storageProvider.getProviderType()).thenReturn(ProviderType.R2);
            when(storageProvider.createUploadSession(any())).thenReturn(providerSession);
            when(assetRepository.save(any())).thenAnswer(inv -> {
                MediaAsset a = inv.getArgument(0);
                setField(a, "publicId", assetPublicId);
                setField(a, "id", 1L);
                return a;
            });
            when(sessionRepository.save(any())).thenAnswer(inv -> {
                MediaUploadSession s = inv.getArgument(0);
                setField(s, "publicId", sessionPublicId);
                return s;
            });

            UploadSessionResponse response = service.initiateUpload(request, userId);

            assertThat(response.uploadId()).isEqualTo(sessionPublicId);
            assertThat(response.mediaId()).isEqualTo(assetPublicId);
            assertThat(response.provider()).isEqualTo(ProviderType.R2);
            assertThat(response.uploadUrl()).isEqualTo("https://r2.example.com/presigned");

            verify(assetRepository, times(2)).save(any());
            verify(sessionRepository).save(any());
            verify(auditLogService).log(any(AuditLogEntry.class));
        }

        @Test
        @DisplayName("Rejected — unsupported MIME type")
        void initiateUpload_invalidMimeType_throwsBadRequest() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "evil.exe", "application/octet-stream", 1024L, null, null,
                    MediaType.PRODUCT_IMAGE, MediaOwnerType.PRODUCT,
                    UUID.randomUUID(), null
            );

            assertThatThrownBy(() -> service.initiateUpload(request, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported media type");
        }

        @Test
        @DisplayName("Rejected — file size exceeds limit")
        void initiateUpload_fileTooLarge_throwsBadRequest() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "huge.jpg", "image/jpeg", 999_999_999L, null, null,
                    MediaType.PRODUCT_IMAGE, MediaOwnerType.PRODUCT,
                    UUID.randomUUID(), null
            );

            assertThatThrownBy(() -> service.initiateUpload(request, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("Rejected — storage quota exceeded")
        void initiateUpload_quotaExceeded_throwsStorageQuotaExceeded() {
            long quota = 2_147_483_648L;
            quotaConfig.setDefaultSellerQuotaBytes(quota);

            StorageUsage usage = StorageUsage.builder()
                    .storageBytes(quota - 100L)
                    .objectCount(10L)
                    .bandwidthBytes(0L)
                    .build();

            InitiateUploadRequest request = new InitiateUploadRequest(
                    "photo.jpg", "image/jpeg", 5000L, null, null,
                    MediaType.PRODUCT_IMAGE, MediaOwnerType.PRODUCT,
                    UUID.randomUUID(), null
            );

            when(usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.of(usage));

            assertThatThrownBy(() -> service.initiateUpload(request, userId))
                    .isInstanceOf(StorageQuotaExceededException.class);
        }
    }

    // ── completeUpload ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeUpload(sessionId, request, userId)")
    class CompleteUpload {

        @Test
        @DisplayName("Happy path — object verified, asset transitions to READY")
        void completeUpload_happyPath_assetBecomesReady() {
            CompleteUploadRequest request = new CompleteUploadRequest(1024L, "etag123", "sha256abc");

            UploadResult result = new UploadResult(
                    "product/uuid/original.jpg", "etag123", "sha256abc", 1024L,
                    "https://cdn.fynza.com/product/uuid/original.jpg"
            );

            when(sessionRepository.findByPublicIdAndUserId(sessionPublicId, userId))
                    .thenReturn(Optional.of(uploadSession));
            when(providerRouter.resolve()).thenReturn(storageProvider);
            when(storageProvider.verifyUpload(any())).thenReturn(result);
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(usageRepository.findForUpdate(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.empty());
            when(usageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MediaAssetResponse response = service.completeUpload(sessionPublicId, request, userId);

            assertThat(response.status()).isEqualTo(MediaStatus.READY);
            assertThat(response.cdnUrl()).isEqualTo("https://cdn.fynza.com/product/uuid/original.jpg");
            assertThat(asset.getEtag()).isEqualTo("etag123");
            assertThat(asset.getChecksum()).isEqualTo("sha256abc");

            verify(auditLogService).log(any(AuditLogEntry.class));
        }

        @Test
        @DisplayName("Not found — unknown session ID throws UploadSessionNotFoundException")
        void completeUpload_unknownSession_throwsNotFound() {
            when(sessionRepository.findByPublicIdAndUserId(sessionPublicId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeUpload(sessionPublicId,
                    new CompleteUploadRequest(1024L, null, null), userId))
                    .isInstanceOf(UploadSessionNotFoundException.class);
        }

        @Test
        @DisplayName("Expired session throws BadRequestException")
        void completeUpload_expiredSession_throwsBadRequest() {
            setField(uploadSession, "expiresAt", Instant.now().minusSeconds(1));

            when(sessionRepository.findByPublicIdAndUserId(sessionPublicId, userId))
                    .thenReturn(Optional.of(uploadSession));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> service.completeUpload(sessionPublicId,
                    new CompleteUploadRequest(1024L, null, null), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Wrong state throws BadRequestException")
        void completeUpload_wrongState_throwsBadRequest() {
            setField(uploadSession, "status", UploadSessionStatus.CANCELLED);

            when(sessionRepository.findByPublicIdAndUserId(sessionPublicId, userId))
                    .thenReturn(Optional.of(uploadSession));

            assertThatThrownBy(() -> service.completeUpload(sessionPublicId,
                    new CompleteUploadRequest(1024L, null, null), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("completable state");
        }
    }

    // ── cancelUpload ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelUpload(sessionId, userId)")
    class CancelUpload {

        @Test
        @DisplayName("Session and associated asset are marked cancelled/deleted")
        void cancelUpload_happyPath_sessionAndAssetCancelled() {
            when(sessionRepository.findByPublicIdAndUserId(sessionPublicId, userId))
                    .thenReturn(Optional.of(uploadSession));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.cancelUpload(sessionPublicId, userId);

            assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.CANCELLED);
            assertThat(asset.getStatus()).isEqualTo(MediaStatus.DELETED);
            assertThat(asset.getIsActive()).isFalse();
            verify(auditLogService).log(any(AuditLogEntry.class));
        }

        @Test
        @DisplayName("Unknown session throws UploadSessionNotFoundException")
        void cancelUpload_unknownSession_throwsNotFound() {
            when(sessionRepository.findByPublicIdAndUserId(sessionPublicId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelUpload(sessionPublicId, userId))
                    .isInstanceOf(UploadSessionNotFoundException.class);
        }
    }

    // ── deleteAsset ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAsset(publicId, userId)")
    class DeleteAsset {

        @Test
        @DisplayName("Owner can delete — asset soft-deleted and storage usage decremented")
        void deleteAsset_owner_softDeleteAndUsageDecrement() {
            StorageUsage usage = StorageUsage.builder()
                    .ownerId(userId).ownerType(MediaOwnerType.USER)
                    .storageBytes(2000L).objectCount(2L).build();

            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));
            when(providerRouter.resolve()).thenReturn(storageProvider);
            doNothing().when(storageProvider).deleteObject(any());
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(usageRepository.findForUpdate(userId, MediaOwnerType.USER))
                    .thenReturn(Optional.of(usage));
            when(usageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deleteAsset(assetPublicId, userId);

            assertThat(asset.getStatus()).isEqualTo(MediaStatus.DELETED);
            assertThat(asset.getIsActive()).isFalse();
            assertThat(asset.getDeletedAt()).isNotNull();
            verify(storageProvider).deleteObject(any(StorageObject.class));
            verify(auditLogService).log(any(AuditLogEntry.class));
        }

        @Test
        @DisplayName("Non-owner throws ForbiddenException")
        void deleteAsset_nonOwner_throwsForbidden() {
            UUID otherId = UUID.randomUUID();
            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> service.deleteAsset(assetPublicId, otherId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Unknown publicId throws MediaAssetNotFoundException")
        void deleteAsset_unknownId_throwsNotFound() {
            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAsset(assetPublicId, userId))
                    .isInstanceOf(MediaAssetNotFoundException.class);
        }
    }

    // ── adminDeleteAsset ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminDeleteAsset(publicId)")
    class AdminDeleteAsset {

        @Test
        @DisplayName("Admin deletes any asset regardless of owner — object removed from storage")
        void adminDeleteAsset_anyOwner_deletedFromStorageAndDb() {
            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));
            when(providerRouter.resolve()).thenReturn(storageProvider);
            doNothing().when(storageProvider).deleteObject(any());
            when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adminDeleteAsset(assetPublicId);

            assertThat(asset.getStatus()).isEqualTo(MediaStatus.DELETED);
            assertThat(asset.getIsActive()).isFalse();
            verify(storageProvider).deleteObject(any(StorageObject.class));
            verify(auditLogService).log(any(AuditLogEntry.class));
        }

        @Test
        @DisplayName("Unknown publicId throws MediaAssetNotFoundException")
        void adminDeleteAsset_unknownId_throwsNotFound() {
            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adminDeleteAsset(assetPublicId))
                    .isInstanceOf(MediaAssetNotFoundException.class);
        }
    }

    // ── generateSignedUrl ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateSignedUrl(publicId, request, userId)")
    class GenerateSignedUrl {

        @Test
        @DisplayName("Public asset with CDN URL — returns CDN URL directly")
        void generateSignedUrl_publicWithCdn_returnsCdnUrl() {
            setField(asset, "cdnUrl", "https://cdn.fynza.com/object.jpg");
            setField(asset, "visibility", MediaVisibility.PUBLIC);

            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));

            GenerateSignedUrlRequest request = GenerateSignedUrlRequest.builder().expirySeconds(600).build();
            SignedUrlResponse response = service.generateSignedUrl(assetPublicId, request, userId);

            assertThat(response.url()).isEqualTo("https://cdn.fynza.com/object.jpg");
            verify(storageProvider, never()).createDownloadUrl(any(), any());
        }

        @Test
        @DisplayName("Private asset — signed URL generated via provider")
        void generateSignedUrl_privateAsset_signsUrl() {
            setField(asset, "visibility", MediaVisibility.PRIVATE);

            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));
            when(providerRouter.resolve()).thenReturn(storageProvider);
            when(storageProvider.createDownloadUrl(any(), any())).thenReturn("https://r2.example.com/signed?token=xyz");

            GenerateSignedUrlRequest request = GenerateSignedUrlRequest.builder().expirySeconds(300).build();
            SignedUrlResponse response = service.generateSignedUrl(assetPublicId, request, userId);

            assertThat(response.url()).isEqualTo("https://r2.example.com/signed?token=xyz");
        }
    }

    // ── attachToProduct ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("attachToProduct(productId, request, userId)")
    class AttachToProduct {

        @Test
        @DisplayName("Primary flag set — clears existing primary before saving")
        void attachToProduct_asPrimary_clearsPreviousPrimary() {
            UUID productId = UUID.randomUUID();
            AttachProductMediaRequest request = new AttachProductMediaRequest(assetPublicId, true, "alt text", 0);

            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));
            when(productMediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.attachToProduct(productId, request, userId);

            verify(productMediaRepository).clearPrimaryForProduct(productId);
            ArgumentCaptor<ProductMedia> captor = ArgumentCaptor.forClass(ProductMedia.class);
            verify(productMediaRepository).save(captor.capture());
            assertThat(captor.getValue().getIsPrimary()).isTrue();
            verify(auditLogService).log(any(AuditLogEntry.class));
        }

        @Test
        @DisplayName("Non-primary — does not clear existing primary")
        void attachToProduct_notPrimary_doesNotClearPrimary() {
            UUID productId = UUID.randomUUID();
            AttachProductMediaRequest request = new AttachProductMediaRequest(assetPublicId, false, null, 1);

            when(assetRepository.findByPublicId(assetPublicId)).thenReturn(Optional.of(asset));
            when(productMediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.attachToProduct(productId, request, userId);

            verify(productMediaRepository, never()).clearPrimaryForProduct(any());
        }
    }

    // ── reorderProductMedia ───────────────────────────────────────────────────

    @Nested
    @DisplayName("reorderProductMedia(productId, orderedIds, userId)")
    class ReorderProductMedia {

        @Test
        @DisplayName("Sort order updated to match supplied list position")
        void reorderProductMedia_updatesOrder() {
            UUID productId   = UUID.randomUUID();
            UUID assetId2    = UUID.randomUUID();

            MediaAsset asset2 = MediaAsset.builder()
                    .ownerId(productId).ownerType(MediaOwnerType.PRODUCT)
                    .provider(ProviderType.R2).mimeType("image/jpeg")
                    .mediaType(MediaType.PRODUCT_IMAGE).fileSize(512L)
                    .visibility(MediaVisibility.PUBLIC).status(MediaStatus.READY)
                    .uploadedBy(userId).objectKey("k2").build();
            setField(asset2, "publicId", assetId2);

            ProductMedia pm1 = ProductMedia.builder().productId(productId).mediaAsset(asset).sortOrder(1).isPrimary(true).build();
            ProductMedia pm2 = ProductMedia.builder().productId(productId).mediaAsset(asset2).sortOrder(0).isPrimary(false).build();

            when(productMediaRepository.findByProductIdOrderBySortOrderAsc(productId))
                    .thenReturn(List.of(pm1, pm2));
            when(productMediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.reorderProductMedia(productId, List.of(assetPublicId, assetId2), userId);

            assertThat(pm1.getSortOrder()).isZero();
            assertThat(pm2.getSortOrder()).isEqualTo(1);
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
