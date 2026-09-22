package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.InventoryLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long> {

    Optional<InventoryLocation> findByPublicId(UUID publicId);

    Optional<InventoryLocation> findByPublicIdAndSellerId(UUID publicId, Long sellerId);

    List<InventoryLocation> findBySellerIdAndIsActiveTrue(Long sellerId);

    Page<InventoryLocation> findBySellerId(Long sellerId, Pageable pageable);

    boolean existsBySellerIdAndCode(Long sellerId, String code);
}
