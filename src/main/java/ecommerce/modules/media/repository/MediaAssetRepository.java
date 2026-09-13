package ecommerce.modules.media.repository;

import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.enums.MediaOwnerType;
import ecommerce.modules.media.enums.MediaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByPublicId(UUID publicId);

    List<MediaAsset> findByOwnerIdAndOwnerTypeAndIsActiveTrue(UUID ownerId, MediaOwnerType ownerType);

    Page<MediaAsset> findByUploadedByAndIsActiveTrue(UUID uploadedBy, Pageable pageable);

    @Query("SELECT m FROM MediaAsset m WHERE m.status = :status AND m.isActive = true")
    Page<MediaAsset> findByStatus(@Param("status") MediaStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaAsset m WHERE m.uploadedBy = :userId AND m.isActive = true AND m.status = 'READY'")
    Long sumFileSizeByUploadedBy(@Param("userId") UUID userId);
}
