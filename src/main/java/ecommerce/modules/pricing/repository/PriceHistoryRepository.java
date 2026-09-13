package ecommerce.modules.pricing.repository;

import ecommerce.modules.pricing.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByPrice_IdOrderByCreatedAtDesc(Long priceId);
}
