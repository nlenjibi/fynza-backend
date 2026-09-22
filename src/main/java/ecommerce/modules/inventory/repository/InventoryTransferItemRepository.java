package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.InventoryTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransferItemRepository extends JpaRepository<InventoryTransferItem, Long> {

    List<InventoryTransferItem> findByTransferId(Long transferId);
}
