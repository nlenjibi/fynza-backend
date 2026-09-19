package ecommerce.modules.order.repository;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.SellerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerOrderRepository extends JpaRepository<SellerOrder, Long> {

    Optional<SellerOrder> findByPublicId(UUID publicId);

    List<SellerOrder> findByOrder_PublicId(UUID orderPublicId);

    Page<SellerOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    long countBySellerId(Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, OrderStatus status);

    @Query("SELECT so FROM SellerOrder so WHERE so.sellerId = :sellerId AND so.order.publicId = :orderPublicId")
    Optional<SellerOrder> findBySellerIdAndOrderPublicId(@Param("sellerId") Long sellerId, @Param("orderPublicId") UUID orderPublicId);
}
