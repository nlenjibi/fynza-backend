package ecommerce.modules.media.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
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
import ecommerce.modules.media.entity.*;
import ecommerce.modules.media.enums.*;
import ecommerce.modules.media.exception.MediaAssetNotFoundException;
import ecommerce.modules.media.exception.StorageQuotaExceededException;
import ecommerce.modules.media.exception.UploadSessionNotFoundException;
import ecommerce.modules.media.provider.*;
import ecommerce.modules.media.repository.MediaAssetRepository;
import ecommerce.modules.media.repository.MediaUploadSessionRepository;
import ecommerce.modules.media.repository.ProductMediaMappingRepository;
import ecommerce.modules.media.repository.StorageUsageRepository;
import ecommerce.modules.media.service.MediaService;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaServiceImpl implements MediaService {

    private final MediaAssetRepository       assetRepository;
    private final MediaUploadSessionRepository sessionRepository;
    private final ProductMediaMappingRepository productMediaRepository;
    private final StorageUsageRepository     usageRepository;
    private final StorageProviderRouter      providerRouter;
    private final MediaProperties            props;
    private final AuditLogService            auditLogService;

    @Override
    @Transactional
    public UploadSessionResponse initiateUpload(InitiateUploadRequest request, UUID userId) {
        validateMimeType(request.contentType());
        validateFileSize(request.size());

        enforceQuota(userId, request.size());

        MediaStorageProvider provider = providerRouter.resolve();

        MediaAsset asset = MediaAsset.builder()
                .ownerId(request.ownerId())
                .ownerType(request.ownerType())
                .provider(provider.getProviderType())
                .originalFilename(request.filename())
                .mimeType(request.contentType())
                .mediaType(request.mediaType())
                .fileSize(request.size())
                .width(request.width())
                .height(request.height())
                .visibility(request.visibility() != null ? request.visibility() : MediaVisibility.PUBLIC)
                .status(MediaStatus.PENDING)
                .uploadedBy(userId)
                .build();
        asset = assetRepository.save(asset);

        UploadRequest uploadRequest = new UploadRequest(
                userId,
                request.ownerId(),
                request.ownerType(),
                request.mediaType(),
                request.filename(),
                request.contentType(),
                request.size(),
                request.width(),
                request.height(),
                asset.getVisibility()
        );
        UploadSession session = provider.createUploadSession(uploadRequest);

        MediaUploadSession sessionEntity = MediaUploadSession.builder()
                .userId(userId)
                .mediaAsset(asset)
                .provider(provider.getProviderType())
                .objectKey(session.objectKey())
                .filename(request.filename())
                .mimeType(request.contentType())
                .expectedSize(request.size())
                .status(UploadSessionStatus.AUTHORIZED)
                .uploadUrl(session.uploadUrl())
                .expiresAt(session.expiresAt())
                .build();
        sessionEntity = sessionRepository.save(sessionEntity);

        asset.setObjectKey(session.objectKey());
        asset.setStatus(MediaStatus.UPLOADING);
        asset.setUploadMethod(session.uploadMethod());
        assetRepository.save(asset);

        audit(AuditAction.MEDIA_UPLOAD_SESSION_INITIATED, "MEDIA_ASSET", asset.getPublicId(), userId);

        return new UploadSessionResponse(
                sessionEntity.getPublicId(),
                asset.getPublicId(),
                provider.getProviderType(),
                session.uploadMethod(),
                session.uploadUrl(),
                session.expiresAt(),
                session.requiredHeaders(),
                session.objectKey()
        );
    }

    @Override
    @Transactional
    public MediaAssetResponse completeUpload(UUID sessionId, CompleteUploadRequest request, UUID userId) {
        MediaUploadSession session = sessionRepository.findByPublicIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new UploadSessionNotFoundException(sessionId));

        if (session.getStatus() != UploadSessionStatus.AUTHORIZED
                && session.getStatus() != UploadSessionStatus.UPLOADING) {
            throw new BadRequestException("Upload session is not in a completable state: " + session.getStatus());
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(UploadSessionStatus.EXPIRED);
            sessionRepository.save(session);
            throw new BadRequestException("Upload session has expired");
        }

        MediaStorageProvider provider = providerRouter.resolve();
        UploadSession uploadSession = new UploadSession(
                session.getPublicId(),
                session.getProvider(),
                null,
                session.getObjectKey(),
                null,
                null,
                session.getExpiresAt()
        );
        UploadResult result = provider.verifyUpload(uploadSession);

        if (result.fileSize() > 0 && result.fileSize() != request.size()) {
            throw new BadRequestException(String.format(
                    "File size mismatch: expected %d bytes, got %d", request.size(), result.fileSize()));
        }

        session.setStatus(UploadSessionStatus.VERIFIED);
        session.setActualSize(result.fileSize());
        session.setChecksum(result.checksum());
        session.setCompletedAt(Instant.now());

        MediaAsset asset = session.getMediaAsset();
        asset.setStatus(MediaStatus.READY);
        asset.setEtag(result.etag());
        asset.setChecksum(result.checksum());
        asset.setFileSize(result.fileSize());
        asset.setCdnUrl(result.cdnUrl());
        assetRepository.save(asset);

        session.setStatus(UploadSessionStatus.MEDIA_CREATED);
        sessionRepository.save(session);

        incrementUsage(userId, MediaOwnerType.USER, result.fileSize());

        audit(AuditAction.MEDIA_ASSET_CREATED, "MEDIA_ASSET", asset.getPublicId(), userId);

        return toResponse(asset);
    }

    @Override
    @Transactional
    public void cancelUpload(UUID sessionId, UUID userId) {
        MediaUploadSession session = sessionRepository.findByPublicIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new UploadSessionNotFoundException(sessionId));

        session.setStatus(UploadSessionStatus.CANCELLED);
        sessionRepository.save(session);

        if (session.getMediaAsset() != null) {
            MediaAsset asset = session.getMediaAsset();
            asset.setStatus(MediaStatus.DELETED);
            asset.setIsActive(false);
            assetRepository.save(asset);
        }

        audit(AuditAction.MEDIA_UPLOAD_CANCELLED, "UPLOAD_SESSION", sessionId, userId);
    }

    @Override
    @Transactional
    public void deleteAsset(UUID publicId, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(publicId));

        if (!asset.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You do not own this media asset");
        }

        MediaStorageProvider provider = providerRouter.resolve();
        provider.deleteObject(new StorageObject(getBucket(asset.getProvider()), asset.getObjectKey()));

        asset.setStatus(MediaStatus.DELETED);
        asset.setIsActive(false);
        asset.setDeletedAt(Instant.now());
        assetRepository.save(asset);

        decrementUsage(userId, MediaOwnerType.USER, asset.getFileSize());

        audit(AuditAction.MEDIA_ASSET_DELETED, "MEDIA_ASSET", publicId, userId);
    }

    @Override
    @Transactional
    public void adminDeleteAsset(UUID publicId) {
        MediaAsset asset = assetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(publicId));

        MediaStorageProvider provider = providerRouter.resolve();
        provider.deleteObject(new StorageObject(getBucket(asset.getProvider()), asset.getObjectKey()));

        asset.setStatus(MediaStatus.DELETED);
        asset.setIsActive(false);
        asset.setDeletedAt(Instant.now());
        assetRepository.save(asset);

        decrementUsage(asset.getUploadedBy(), MediaOwnerType.USER, asset.getFileSize());

        audit(AuditAction.MEDIA_ASSET_DELETED, "MEDIA_ASSET", publicId, asset.getUploadedBy());
    }

    @Override
    public SignedUrlResponse generateSignedUrl(UUID publicId, GenerateSignedUrlRequest request, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(publicId));

        if (asset.getVisibility() != MediaVisibility.PUBLIC && !asset.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You do not have access to this media asset");
        }

        if (asset.getVisibility() == MediaVisibility.PUBLIC && asset.getCdnUrl() != null) {
            return new SignedUrlResponse(asset.getCdnUrl(), Instant.now().plusSeconds(request.getExpirySeconds()));
        }

        MediaStorageProvider provider = providerRouter.resolve();
        Duration expiry = Duration.ofSeconds(request.getExpirySeconds());
        String url = provider.createDownloadUrl(
                new StorageObject(getBucket(asset.getProvider()), asset.getObjectKey()),
                expiry
        );
        return new SignedUrlResponse(url, Instant.now().plus(expiry));
    }

    @Override
    public SignedUrlResponse getSignedDownloadUrl(UUID publicId, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(publicId));

        if (asset.getVisibility() != MediaVisibility.PUBLIC && !asset.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You do not have access to this media asset");
        }

        if (asset.getVisibility() == MediaVisibility.PUBLIC && asset.getCdnUrl() != null) {
            return new SignedUrlResponse(asset.getCdnUrl(), Instant.now().plusSeconds(3600));
        }

        MediaStorageProvider provider = providerRouter.resolve();
        Duration expiry = Duration.ofHours(1);
        String url = provider.createDownloadUrl(
                new StorageObject(getBucket(asset.getProvider()), asset.getObjectKey()),
                expiry
        );
        return new SignedUrlResponse(url, Instant.now().plus(expiry));
    }

    @Override
    @Transactional
    public void attachToProduct(UUID productId, AttachProductMediaRequest request, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(request.mediaAssetId())
                .orElseThrow(() -> new MediaAssetNotFoundException(request.mediaAssetId()));

        if (request.isPrimary()) {
            productMediaRepository.clearPrimaryForProduct(productId);
        }

        ProductMedia pm = ProductMedia.builder()
                .productId(productId)
                .mediaAsset(asset)
                .sortOrder(request.sortOrder())
                .isPrimary(request.isPrimary())
                .altText(request.altText())
                .build();
        productMediaRepository.save(pm);

        audit(AuditAction.PRODUCT_MEDIA_ADDED, "PRODUCT_MEDIA", productId, userId);
    }

    @Override
    @Transactional
    public void detachFromProduct(UUID productId, UUID mediaAssetPublicId, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(mediaAssetPublicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(mediaAssetPublicId));

        productMediaRepository.findByProductIdAndMediaAssetId(productId, asset.getId())
                .ifPresent(productMediaRepository::delete);

        audit(AuditAction.PRODUCT_MEDIA_REMOVED, "PRODUCT_MEDIA", productId, userId);
    }

    @Override
    @Transactional
    public void reorderProductMedia(UUID productId, List<UUID> orderedMediaIds, UUID userId) {
        List<ProductMedia> productMediaList = productMediaRepository.findByProductIdOrderBySortOrderAsc(productId);
        for (int i = 0; i < orderedMediaIds.size(); i++) {
            UUID assetPublicId = orderedMediaIds.get(i);
            int sortOrder = i;
            productMediaList.stream()
                    .filter(pm -> pm.getMediaAsset().getPublicId().equals(assetPublicId))
                    .findFirst()
                    .ifPresent(pm -> {
                        pm.setSortOrder(sortOrder);
                        productMediaRepository.save(pm);
                    });
        }
        audit(AuditAction.PRODUCT_MEDIA_REORDERED, "PRODUCT_MEDIA", productId, userId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validateMimeType(String mimeType) {
        if (!props.getUpload().getAllowedMimeTypes().contains(mimeType)) {
            throw new BadRequestException("Unsupported media type: " + mimeType);
        }
    }

    private void validateFileSize(long size) {
        if (size > props.getUpload().getMaxFileSizeBytes()) {
            throw new BadRequestException(String.format(
                    "File size %d exceeds maximum allowed %d bytes", size, props.getUpload().getMaxFileSizeBytes()
            ));
        }
    }

    private void enforceQuota(UUID userId, long requestedSize) {
        StorageUsage usage = usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER).orElse(null);
        long used = usage != null ? usage.getStorageBytes() : 0L;
        long quota = props.getQuota().getDefaultSellerQuotaBytes();
        if (used + requestedSize > quota) {
            throw new StorageQuotaExceededException(used + requestedSize, quota);
        }
    }

    private void incrementUsage(UUID ownerId, MediaOwnerType ownerType, long bytes) {
        StorageUsage usage = usageRepository.findForUpdate(ownerId, ownerType)
                .orElseGet(() -> StorageUsage.builder().ownerId(ownerId).ownerType(ownerType).build());
        usage.setStorageBytes(usage.getStorageBytes() + bytes);
        usage.setObjectCount(usage.getObjectCount() + 1);
        usageRepository.save(usage);
    }

    private void decrementUsage(UUID ownerId, MediaOwnerType ownerType, long bytes) {
        usageRepository.findForUpdate(ownerId, ownerType).ifPresent(usage -> {
            usage.setStorageBytes(Math.max(0, usage.getStorageBytes() - bytes));
            usage.setObjectCount(Math.max(0, usage.getObjectCount() - 1));
            usageRepository.save(usage);
        });
    }

    private void audit(String action, String entityType, UUID entityId, UUID actorId) {
        auditLogService.log(AuditLogEntry.builder()
                .action(action)
                .entityType(entityType)
                .entityPublicId(entityId)
                .actorPublicId(actorId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());
    }

    private String getBucket(ProviderType providerType) {
        return switch (providerType) {
            case R2         -> props.getProvider().getR2().getBucket();
            case S3         -> props.getProvider().getS3().getBucket();
            case SUPABASE   -> props.getProvider().getSupabase().getBucket();
            case FIREBASE   -> props.getProvider().getFirebase().getBucket();
            case CLOUDINARY -> null;
            case LOCAL      -> null;
        };
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getPublicId(),
                asset.getOwnerId(),
                asset.getOwnerType(),
                asset.getProvider(),
                asset.getOriginalFilename(),
                asset.getMimeType(),
                asset.getMediaType(),
                asset.getFileSize(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getVisibility(),
                asset.getStatus(),
                asset.getCdnUrl(),
                asset.getCreatedAt()
        );
    }
}
