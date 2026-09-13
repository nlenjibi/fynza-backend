package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByInventoryId(Long inventoryId, Pageable pageable);

    List<StockMovement> findByInventoryIdAndMovementType(Long inventoryId, MovementType movementType);
}
