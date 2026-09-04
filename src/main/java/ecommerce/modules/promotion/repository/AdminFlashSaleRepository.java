package ecommerce.modules.promotion.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.promotion.entity.AdminFlashSale;
import ecommerce.modules.promotion.entity.AdminFlashSale.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdminFlashSaleRepository extends BaseRepository<AdminFlashSale, UUID> {

    Page<AdminFlashSale> findAllByOrderByStartDatetimeDesc(Pageable pageable);

    Page<AdminFlashSale> findByStatusOrderByStartDatetimeDesc(Status status, Pageable pageable);

    @Query("SELECT f FROM AdminFlashSale f WHERE f.startDatetime <= :now AND f.endDatetime >= :now")
    List<AdminFlashSale> findActive(@Param("now") LocalDateTime now);

    @Query("SELECT f FROM AdminFlashSale f WHERE f.startDatetime > :now AND f.status = 'SCHEDULED'")
    List<AdminFlashSale> findUpcoming(@Param("now") LocalDateTime now);

    @Query("SELECT f FROM AdminFlashSale f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<AdminFlashSale> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(f) FROM AdminFlashSale f WHERE f.status = 'ACTIVE'")
    long countActive();

    @Query("UPDATE AdminFlashSale f SET f.currentProductsCount = f.currentProductsCount + :count WHERE f.id = :id")
    void incrementProductCount(@Param("id") UUID id, @Param("count") int count);

    @Query("UPDATE AdminFlashSale f SET f.currentProductsCount = f.currentProductsCount - :count WHERE f.id = :id AND f.currentProductsCount >= :count")
    void decrementProductCount(@Param("id") UUID id, @Param("count") int count);
}
