package ecommerce.modules.pricing.repository;

import ecommerce.modules.pricing.entity.PriceTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceTierRepository extends JpaRepository<PriceTier, Long> {

    Optional<PriceTier> findByPublicId(UUID publicId);

    List<PriceTier> findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(Long priceId);
}
