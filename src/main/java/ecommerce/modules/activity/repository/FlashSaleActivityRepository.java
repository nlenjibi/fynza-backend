package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.FlashSaleActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FlashSaleActivityRepository extends JpaRepository<FlashSaleActivity, UUID> {

    Page<FlashSaleActivity> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<FlashSaleActivity> findByFlashSaleIdOrderByCreatedAtDesc(UUID flashSaleId, Pageable pageable);

    Page<FlashSaleActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
