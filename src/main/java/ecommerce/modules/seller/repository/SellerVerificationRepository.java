package ecommerce.modules.seller.repository;

import ecommerce.modules.seller.entity.SellerVerification;
import ecommerce.modules.seller.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerVerificationRepository extends JpaRepository<SellerVerification, Long> {

    List<SellerVerification> findBySellerId(Long sellerId);

    Optional<SellerVerification> findBySellerIdAndVerificationType(Long sellerId, VerificationType verificationType);
}
