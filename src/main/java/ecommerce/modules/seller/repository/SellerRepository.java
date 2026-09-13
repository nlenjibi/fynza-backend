package ecommerce.modules.seller.repository;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.entity.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, Long>, JpaSpecificationExecutor<Seller> {

    Optional<Seller> findByPublicId(UUID publicId);

    Optional<Seller> findByOwnerUserId(UUID ownerUserId);

    boolean existsByOwnerUserId(UUID ownerUserId);

    Page<Seller> findAll(Specification<Seller> spec, Pageable pageable);

    long countByStatus(SellerStatus status);
}
