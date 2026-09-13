package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.InventoryTransfer;
import ecommerce.modules.inventory.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Long> {

    Optional<InventoryTransfer> findByPublicId(UUID publicId);

    Page<InventoryTransfer> findByRequestedBy(UUID requestedBy, Pageable pageable);

    Page<InventoryTransfer> findByStatus(TransferStatus status, Pageable pageable);
}
