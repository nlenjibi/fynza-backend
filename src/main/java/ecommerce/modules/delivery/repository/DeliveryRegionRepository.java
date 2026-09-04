package ecommerce.modules.delivery.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.delivery.entity.DeliveryRegion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRegionRepository extends BaseRepository<DeliveryRegion, UUID> {

    Optional<DeliveryRegion> findByCode(String code);

    Optional<DeliveryRegion> findByName(String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query("SELECT r FROM DeliveryRegion r WHERE r.isActive = true")
    List<DeliveryRegion> findAllActive();

    List<DeliveryRegion> findByIsActiveTrue();
}
