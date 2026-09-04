package ecommerce.modules.order.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends BaseRepository<OrderItem, UUID> {

    @EntityGraph(attributePaths = {"product"})
    List<OrderItem> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"product"})
    List<OrderItem> findByProductId(UUID productId);

    @EntityGraph(attributePaths = {"product"})
    List<OrderItem> findByProductIdIn(List<UUID> productIds);

    @Query("""
            SELECT oi.product.id, oi.product.name, SUM(oi.quantity) AS totalSold
            FROM OrderItem oi
            WHERE oi.order.status = 'DELIVERED'
            GROUP BY oi.product.id, oi.product.name
            ORDER BY totalSold DESC
            """)
    List<Object[]> findBestSellingProducts(org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM OrderItem oi
            WHERE oi.product.id = :productId
              AND oi.order.status = 'DELIVERED'
            """)
    long countTotalSoldByProduct(@Param("productId") UUID productId);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.seller.id = :sellerId")
    long countByProductSellerId(@Param("sellerId") UUID sellerId);

    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi WHERE oi.product.seller.id = :sellerId AND oi.order.status IN ('DELIVERED', 'CONFIRMED')")
    BigDecimal sumRevenueBySellerId(@Param("sellerId") UUID sellerId);

    @Query("SELECT oi.product.seller.id, COUNT(oi) FROM OrderItem oi WHERE oi.order.status IN ('DELIVERED', 'CONFIRMED') GROUP BY oi.product.seller.id ORDER BY COUNT(oi) DESC")
    List<Object[]> findTopSellerIdsByOrderCount(org.springframework.data.domain.Pageable pageable);
}
