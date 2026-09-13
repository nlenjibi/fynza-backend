package ecommerce.modules.pricing.repository;

import ecommerce.modules.pricing.entity.PriceOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceOverrideRepository extends JpaRepository<PriceOverride, Long> {

    Optional<PriceOverride> findByPublicId(UUID publicId);

    List<PriceOverride> findByPrice_IdOrderByCreatedAtDesc(Long priceId);
}
