package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Long> {
    Optional<ShippingMethod> findByPublicId(UUID publicId);
    List<ShippingMethod> findByCarrier_PublicIdAndIsActiveTrue(UUID carrierPublicId);
    List<ShippingMethod> findByIsActiveTrue();
    boolean existsByCarrier_IdAndCode(Long carrierId, String code);
}
