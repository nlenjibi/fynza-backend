package ecommerce.modules.delivery.repository;

import ecommerce.modules.delivery.entity.DeliveryRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRegionRepository extends JpaRepository<DeliveryRegion, Long> {

    Optional<DeliveryRegion> findByPublicId(UUID publicId);

    Optional<DeliveryRegion> findByCode(String code);

    Optional<DeliveryRegion> findByName(String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query("SELECT r FROM DeliveryRegion r WHERE r.isActive = true")
    List<DeliveryRegion> findAllActive();

    List<DeliveryRegion> findByIsActiveTrue();
}
