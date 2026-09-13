package ecommerce.modules.media.service;

import ecommerce.modules.media.dto.request.AttachProductMediaRequest;
import ecommerce.modules.media.dto.request.CompleteUploadRequest;
import ecommerce.modules.media.dto.request.InitiateUploadRequest;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.SignedUrlResponse;
import ecommerce.modules.media.dto.response.UploadSessionResponse;

import java.util.List;
import java.util.UUID;

public interface MediaService {

    UploadSessionResponse initiateUpload(InitiateUploadRequest request, UUID userId);

    MediaAssetResponse completeUpload(UUID sessionId, CompleteUploadRequest request, UUID userId);

    void cancelUpload(UUID sessionId, UUID userId);

    void deleteAsset(UUID publicId, UUID userId);

    SignedUrlResponse getSignedDownloadUrl(UUID publicId, UUID userId);

    void attachToProduct(UUID productId, AttachProductMediaRequest request, UUID userId);

    void detachFromProduct(UUID productId, UUID mediaAssetPublicId, UUID userId);

    void reorderProductMedia(UUID productId, List<UUID> orderedMediaIds, UUID userId);
}
