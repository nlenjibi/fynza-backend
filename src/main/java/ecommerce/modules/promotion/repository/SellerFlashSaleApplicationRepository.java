package ecommerce.modules.promotion.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.promotion.entity.SellerFlashSaleApplication;
import ecommerce.modules.promotion.entity.SellerFlashSaleApplication.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerFlashSaleApplicationRepository extends BaseRepository<SellerFlashSaleApplication, UUID> {

    Optional<SellerFlashSaleApplication> findByFlashSaleIdAndSellerId(UUID flashSaleId, UUID sellerId);

    Page<SellerFlashSaleApplication> findBySellerId(UUID sellerId, Pageable pageable);

    Page<SellerFlashSaleApplication> findByFlashSaleId(UUID flashSaleId, Pageable pageable);

    Page<SellerFlashSaleApplication> findByFlashSaleIdAndStatus(UUID flashSaleId, Status status, Pageable pageable);

    @Query("SELECT a FROM SellerFlashSaleApplication a WHERE a.status = 'PENDING'")
    Page<SellerFlashSaleApplication> findPendingApplications(Pageable pageable);

    @Query("SELECT a FROM SellerFlashSaleApplication a WHERE a.flashSaleId = :flashSaleId AND a.status = 'PENDING'")
    Page<SellerFlashSaleApplication> findPendingApplicationsByFlashSaleId(@Param("flashSaleId") UUID flashSaleId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM SellerFlashSaleApplication a WHERE a.flashSaleId = :flashSaleId AND a.status = 'APPROVED'")
    long countApprovedApplications(@Param("flashSaleId") UUID flashSaleId);

    boolean existsByFlashSaleIdAndSellerId(UUID flashSaleId, UUID sellerId);
}
