package ecommerce.modules.media.scheduler;

import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.MediaUploadSession;
import ecommerce.modules.media.enums.MediaStatus;
import ecommerce.modules.media.enums.UploadSessionStatus;
import ecommerce.modules.media.repository.MediaAssetRepository;
import ecommerce.modules.media.repository.MediaUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaUploadSessionExpiryScheduler {

    private final MediaUploadSessionRepository sessionRepository;
    private final MediaAssetRepository         assetRepository;

    @Scheduled(fixedDelayString = "${fynza.media.upload.session-expiry-check-ms:300000}")
    @Transactional
    public void expireStaleUploadSessions() {
        List<MediaUploadSession> expired = sessionRepository.findExpiredSessions(Instant.now());
        if (expired.isEmpty()) return;

        log.info("Expiring {} stale upload sessions", expired.size());
        for (MediaUploadSession session : expired) {
            session.setStatus(UploadSessionStatus.EXPIRED);

            MediaAsset asset = session.getMediaAsset();
            if (asset != null && asset.getStatus() != MediaStatus.READY) {
                asset.setStatus(MediaStatus.EXPIRED);
                asset.setIsActive(false);
                assetRepository.save(asset);
            }
        }
        sessionRepository.saveAll(expired);
    }
}
