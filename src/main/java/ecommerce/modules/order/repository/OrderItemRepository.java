package ecommerce.modules.order.repository;

import ecommerce.modules.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    java.util.Optional<OrderItem> findByPublicId(UUID publicId);

    List<OrderItem> findByOrder_PublicId(UUID orderPublicId);

    List<OrderItem> findBySellerOrder_PublicId(UUID sellerOrderPublicId);

    @Query("""
            SELECT oi.productId, oi.productName, SUM(oi.quantity) AS totalSold
            FROM OrderItem oi
            WHERE oi.order.status = 'DELIVERED'
            GROUP BY oi.productId, oi.productName
            ORDER BY totalSold DESC
            """)
    List<Object[]> findBestSellingProducts(Pageable pageable);

    long countBySellerId(Long sellerId);

    @Query("SELECT COALESCE(SUM(oi.subtotal), 0) FROM OrderItem oi WHERE oi.sellerId = :sellerId AND oi.order.status IN ('DELIVERED', 'CONFIRMED')")
    java.math.BigDecimal sumRevenueBySellerId(@Param("sellerId") Long sellerId);
}
