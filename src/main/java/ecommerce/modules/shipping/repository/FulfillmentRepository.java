package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.Fulfillment;
import ecommerce.modules.shipping.enums.FulfillmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {
    Optional<Fulfillment> findByPublicId(UUID publicId);
    Optional<Fulfillment> findBySellerOrderId(UUID sellerOrderId);
    List<Fulfillment> findByOrderId(UUID orderId);
    Page<Fulfillment> findBySellerId(Long sellerId, Pageable pageable);
    Page<Fulfillment> findBySellerIdAndStatus(Long sellerId, FulfillmentStatus status, Pageable pageable);
    boolean existsBySellerOrderId(UUID sellerOrderId);
}
