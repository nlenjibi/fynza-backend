package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    Optional<Carrier> findByPublicId(UUID publicId);
    Optional<Carrier> findByCode(String code);
    boolean existsByCode(String code);
    List<Carrier> findByIsActiveTrue();
}
