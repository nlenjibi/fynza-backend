package ecommerce.modules.media.repository;

import ecommerce.modules.media.entity.MediaUploadSession;
import ecommerce.modules.media.enums.UploadSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaUploadSessionRepository extends JpaRepository<MediaUploadSession, Long> {

    Optional<MediaUploadSession> findByPublicId(UUID publicId);

    Optional<MediaUploadSession> findByPublicIdAndUserId(UUID publicId, UUID userId);

    @Query("SELECT s FROM MediaUploadSession s WHERE s.status IN ('CREATED','AUTHORIZED','UPLOADING') AND s.expiresAt < :now")
    List<MediaUploadSession> findExpiredSessions(@Param("now") Instant now);
}
