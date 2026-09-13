package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.CreateTransferRequest;
import ecommerce.modules.inventory.dto.request.ReceiveTransferRequest;
import ecommerce.modules.inventory.dto.response.InventoryTransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryTransferService {

    InventoryTransferResponse requestTransfer(CreateTransferRequest request, UUID requestedBy);

    InventoryTransferResponse approveTransfer(UUID transferPublicId, UUID approvedBy);

    InventoryTransferResponse receiveTransfer(UUID transferPublicId, ReceiveTransferRequest request, UUID receivedBy);

    InventoryTransferResponse cancelTransfer(UUID transferPublicId);

    Page<InventoryTransferResponse> getTransfers(Pageable pageable);
}
