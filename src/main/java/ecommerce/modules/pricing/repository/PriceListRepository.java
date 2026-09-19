package ecommerce.modules.pricing.repository;

import ecommerce.modules.pricing.entity.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    Optional<PriceList> findByPublicId(UUID publicId);

    Optional<PriceList> findByIsDefaultTrue();
}
