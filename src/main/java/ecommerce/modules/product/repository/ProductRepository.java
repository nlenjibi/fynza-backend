package ecommerce.modules.product.repository;

import ecommerce.common.enums.InventoryStatus;
import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByPublicId(UUID publicId);

    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByBrandIgnoreCase(String brand, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByBrandInIgnoreCase(List<String> brands, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByCategory_PublicId(UUID categoryPublicId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findBySeller_PublicId(UUID sellerPublicId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.rating >= :minRating ORDER BY p.rating DESC")
    List<Product> findFeatured(@Param("status") ProductStatus status, @Param("minRating") Double minRating);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.viewCount DESC")
    List<Product> findTopByViewCount(@Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.seller WHERE p.id IN :ids")
    List<Product> findByIdIn(@Param("ids") List<Long> ids);

    long countByInventoryStatusAndIsActiveTrue(InventoryStatus inventoryStatus);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") ProductStatus status);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity, p.availableQuantity = p.availableQuantity - :quantity WHERE p.id = :id AND p.isActive = true AND p.stock >= :quantity")
    int reserveStockAndIsActiveTrue(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.availableQuantity = p.availableQuantity + :quantity WHERE p.id = :id AND p.isActive = true")
    int releaseReservedStockAndIsActiveTrue(@Param("id") Long id, @Param("quantity") int quantity);

    long countBySellerId(Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, ProductStatus status);

    long countBySellerIdAndInventoryStatus(Long sellerId, InventoryStatus inventoryStatus);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId AND p.inventoryStatus = 'LOW_STOCK'")
    long countBySellerIdAndLowStock(@Param("sellerId") Long sellerId);

    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findByStatusAndIsApproved(ProductStatus status, Boolean isApproved, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'PENDING'")
    long countPendingProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'PENDING' AND p.isApproved = false")
    long countPendingApproval();
}
