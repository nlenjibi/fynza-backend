package ecommerce.modules.seller.repository;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.entity.SellerStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SellerStatusHistoryRepository extends JpaRepository<SellerStatusHistory, Long> {

    List<SellerStatusHistory> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("SELECT h FROM SellerStatusHistory h WHERE h.newStatus = :status AND h.expiresAt IS NOT NULL AND h.expiresAt < :now")
    List<SellerStatusHistory> findExpiredSuspensions(@Param("status") SellerStatus status, @Param("now") Instant now);
}
