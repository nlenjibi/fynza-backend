package ecommerce.modules.media.scheduler;

import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.MediaUploadSession;
import ecommerce.modules.media.enums.MediaOwnerType;
import ecommerce.modules.media.enums.MediaStatus;
import ecommerce.modules.media.enums.MediaType;
import ecommerce.modules.media.enums.MediaVisibility;
import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadSessionStatus;
import ecommerce.modules.media.repository.MediaAssetRepository;
import ecommerce.modules.media.repository.MediaUploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaUploadSessionExpiryScheduler")
class MediaUploadSessionExpirySchedulerTest {

    @Mock private MediaUploadSessionRepository sessionRepository;
    @Mock private MediaAssetRepository         assetRepository;

    @InjectMocks
    private MediaUploadSessionExpiryScheduler scheduler;

    private MediaAsset uploadingAsset;

    @BeforeEach
    void setUp() {
        uploadingAsset = MediaAsset.builder()
                .ownerId(UUID.randomUUID())
                .ownerType(MediaOwnerType.PRODUCT)
                .provider(ProviderType.R2)
                .originalFilename("img.jpg")
                .mimeType("image/jpeg")
                .mediaType(MediaType.PRODUCT_IMAGE)
                .fileSize(1024L)
                .visibility(MediaVisibility.PUBLIC)
                .status(MediaStatus.UPLOADING)
                .uploadedBy(UUID.randomUUID())
                .objectKey("product/uuid/original.jpg")
                .build();
        setField(uploadingAsset, "id", 1L);
        setField(uploadingAsset, "publicId", UUID.randomUUID());
        setField(uploadingAsset, "isActive", true);
        setField(uploadingAsset, "updatedAt", Instant.now());
    }

    @Test
    @DisplayName("Expired sessions — status set to EXPIRED, associated non-READY assets soft-deleted")
    void expireStaleUploadSessions_withExpiredSessions_updatesStatusAndAsset() {
        MediaUploadSession session = MediaUploadSession.builder()
                .userId(UUID.randomUUID())
                .mediaAsset(uploadingAsset)
                .provider(ProviderType.R2)
                .objectKey("product/uuid/original.jpg")
                .filename("img.jpg")
                .mimeType("image/jpeg")
                .expectedSize(1024L)
                .status(UploadSessionStatus.AUTHORIZED)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        setField(session, "publicId", UUID.randomUUID());

        when(sessionRepository.findExpiredSessions(any())).thenReturn(List.of(session));
        when(sessionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.expireStaleUploadSessions();

        assertThat(session.getStatus()).isEqualTo(UploadSessionStatus.EXPIRED);
        assertThat(uploadingAsset.getStatus()).isEqualTo(MediaStatus.EXPIRED);
        assertThat(uploadingAsset.getIsActive()).isFalse();

        verify(sessionRepository).saveAll(List.of(session));
        verify(assetRepository).save(uploadingAsset);
    }

    @Test
    @DisplayName("READY asset not touched — only non-READY assets are marked expired")
    void expireStaleUploadSessions_readyAsset_assetNotModified() {
        setField(uploadingAsset, "status", MediaStatus.READY);

        MediaUploadSession session = MediaUploadSession.builder()
                .userId(UUID.randomUUID())
                .mediaAsset(uploadingAsset)
                .provider(ProviderType.R2)
                .objectKey("product/uuid/original.jpg")
                .filename("img.jpg")
                .mimeType("image/jpeg")
                .expectedSize(1024L)
                .status(UploadSessionStatus.AUTHORIZED)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        setField(session, "publicId", UUID.randomUUID());

        when(sessionRepository.findExpiredSessions(any())).thenReturn(List.of(session));
        when(sessionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.expireStaleUploadSessions();

        assertThat(session.getStatus()).isEqualTo(UploadSessionStatus.EXPIRED);
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("No expired sessions — repositories not called for saves")
    void expireStaleUploadSessions_noExpired_noSaves() {
        when(sessionRepository.findExpiredSessions(any())).thenReturn(List.of());

        scheduler.expireStaleUploadSessions();

        verify(sessionRepository, never()).saveAll(any());
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Session without associated asset — no NPE, session still marked expired")
    void expireStaleUploadSessions_noAsset_sessionExpiredSafely() {
        MediaUploadSession session = MediaUploadSession.builder()
                .userId(UUID.randomUUID())
                .provider(ProviderType.R2)
                .objectKey("product/uuid/original.jpg")
                .filename("img.jpg")
                .mimeType("image/jpeg")
                .expectedSize(1024L)
                .status(UploadSessionStatus.AUTHORIZED)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        setField(session, "publicId", UUID.randomUUID());

        when(sessionRepository.findExpiredSessions(any())).thenReturn(List.of(session));
        when(sessionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.expireStaleUploadSessions();

        assertThat(session.getStatus()).isEqualTo(UploadSessionStatus.EXPIRED);
        verify(assetRepository, never()).save(any());
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
