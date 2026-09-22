package ecommerce.modules.media.repository;

import ecommerce.modules.media.entity.StorageUsage;
import ecommerce.modules.media.enums.MediaOwnerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, Long> {

    Optional<StorageUsage> findByOwnerIdAndOwnerType(UUID ownerId, MediaOwnerType ownerType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StorageUsage s WHERE s.ownerId = :ownerId AND s.ownerType = :ownerType")
    Optional<StorageUsage> findForUpdate(@Param("ownerId") UUID ownerId,
                                         @Param("ownerType") MediaOwnerType ownerType);
}
