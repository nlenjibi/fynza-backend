package ecommerce.modules.seller.repository;

import ecommerce.modules.seller.entity.SellerBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerBusinessRepository extends JpaRepository<SellerBusiness, Long> {

    Optional<SellerBusiness> findBySellerId(Long sellerId);
}
