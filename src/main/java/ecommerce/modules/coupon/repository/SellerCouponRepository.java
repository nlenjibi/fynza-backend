package ecommerce.modules.coupon.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.coupon.entity.SellerCoupon;
import ecommerce.modules.coupon.entity.SellerCoupon.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerCouponRepository extends BaseRepository<SellerCoupon, UUID> {

    List<SellerCoupon> findBySellerId(UUID sellerId);

    Page<SellerCoupon> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<SellerCoupon> findBySellerIdAndStatusOrderByCreatedAtDesc(UUID sellerId, Status status, Pageable pageable);

    Optional<SellerCoupon> findByCodeAndSellerId(String code, UUID sellerId);

    @Query("SELECT c FROM SellerCoupon c WHERE c.sellerId = :sellerId AND c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validUntil >= :now")
    List<SellerCoupon> findValidCoupons(@Param("sellerId") UUID sellerId, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM SellerCoupon c WHERE LOWER(c.code) = LOWER(:code) AND c.sellerId = :sellerId")
    Optional<SellerCoupon> findByCodeIgnoreCase(@Param("code") String code, @Param("sellerId") UUID sellerId);

    @Query("SELECT c FROM SellerCoupon c WHERE c.sellerId = :sellerId AND c.status = 'ACTIVE'")
    List<SellerCoupon> findActiveBySellerId(UUID sellerId);
}
