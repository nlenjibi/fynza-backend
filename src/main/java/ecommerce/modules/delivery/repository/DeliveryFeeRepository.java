package ecommerce.modules.delivery.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.common.enums.DeliveryMethod;
import ecommerce.modules.delivery.entity.DeliveryFee;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryFeeRepository extends BaseRepository<DeliveryFee, UUID> {

    Optional<DeliveryFee> findByTownName(String townName);

    List<DeliveryFee> findByRegionId(UUID regionId);

    List<DeliveryFee> findByIsActiveTrue();

    @Query("SELECT f FROM DeliveryFee f WHERE f.isActive = true")
    List<DeliveryFee> findAllActive();

    boolean existsByTownNameAndDeliveryMethod(String townName, DeliveryMethod method);
}
