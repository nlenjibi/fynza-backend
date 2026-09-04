package ecommerce.modules.follow.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.follow.entity.StoreFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreFollowRepository extends BaseRepository<StoreFollow, UUID> {

    Page<StoreFollow> findBySellerId(UUID sellerId, Pageable pageable);

    Page<StoreFollow> findByCustomerId(UUID customerId, Pageable pageable);

    Optional<StoreFollow> findByCustomerIdAndSellerId(UUID customerId, UUID sellerId);

    boolean existsByCustomerIdAndSellerId(UUID customerId, UUID sellerId);

    long countBySellerId(UUID sellerId);

    long countByCustomerId(UUID customerId);

    @Query("SELECT COUNT(sf) FROM StoreFollow sf WHERE sf.seller.id = :sellerId AND sf.followedAt >= :since")
    long countBySellerIdAndFollowedAtSince(@Param("sellerId") UUID sellerId, @Param("since") java.time.LocalDateTime since);

    void deleteByCustomerIdAndSellerId(UUID customerId, UUID sellerId);
}
