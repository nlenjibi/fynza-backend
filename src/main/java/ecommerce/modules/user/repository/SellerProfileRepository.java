package ecommerce.modules.user.repository;

import ecommerce.common.enums.SellerStatus;
import ecommerce.common.enums.VerificationStatus;
import ecommerce.modules.user.entity.SellerProfile;
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
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByPublicId(UUID publicId);

    Optional<SellerProfile> findByUser_PublicId(UUID userPublicId);

    List<SellerProfile> findByUser_PublicIdIn(List<UUID> userPublicIds);

    @Query("SELECT sp FROM SellerProfile sp WHERE sp.verificationStatus = :status")
    Page<SellerProfile> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    @Query("SELECT sp FROM SellerProfile sp ORDER BY sp.rating DESC")
    Page<SellerProfile> findTopSellers(Pageable pageable);

    @Query("SELECT sp FROM SellerProfile sp WHERE " +
           "(:query IS NULL OR LOWER(sp.storeName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(sp.user.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR sp.sellerStatus = :status)")
    Page<SellerProfile> searchSellers(@Param("query") String query, @Param("status") SellerStatus status, Pageable pageable);

    long countBySellerStatus(SellerStatus status);

    @Query("SELECT COUNT(sp) FROM SellerProfile sp")
    long countAllSellers();
}
