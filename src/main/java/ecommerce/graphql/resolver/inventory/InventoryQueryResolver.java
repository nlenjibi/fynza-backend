package ecommerce.graphql.resolver.inventory;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.inventory.dto.response.AvailabilityResponse;
import ecommerce.modules.inventory.dto.response.InventoryLocationResponse;
import ecommerce.modules.inventory.dto.response.InventoryResponse;
import ecommerce.modules.inventory.dto.response.InventoryTransferResponse;
import ecommerce.modules.inventory.dto.response.StockMovementResponse;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.InventoryLocationService;
import ecommerce.modules.inventory.service.InventoryService;
import ecommerce.modules.inventory.service.InventoryTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class InventoryQueryResolver {

    private final InventoryService          inventoryService;
    private final InventoryLocationService  locationService;
    private final InventoryTransferService  transferService;
    private final StockMovementRepository   movementRepository;

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory.read')")
    public InventoryResponse inventory(@Argument String publicId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL inventory publicId={}", publicId);
        return inventoryService.getInventory(UUID.fromString(publicId), principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory.read')")
    public List<InventoryResponse> myInventory(@Argument Integer page,
                                               @Argument Integer size,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myInventory");
        return inventoryService.getSellerInventory(principal.getId(),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AvailabilityResponse availability(@Argument String productId, @Argument String variantId) {
        log.debug("GQL availability productId={} variantId={}", productId, variantId);
        return inventoryService.getAvailability(
                UUID.fromString(productId),
                variantId != null ? UUID.fromString(variantId) : null);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory.history.read')")
    public Page<StockMovementResponse> inventoryMovements(@Argument String inventoryId,
                                                          @Argument Integer page,
                                                          @Argument Integer size) {
        log.debug("GQL inventoryMovements inventoryId={}", inventoryId);
        return movementRepository.findByInventoryId(
                Long.parseLong(inventoryId),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(StockMovementResponse::from);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory.location.read')")
    public List<InventoryLocationResponse> inventoryLocations(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL inventoryLocations");
        return locationService.getSellerLocations(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory.transfer.create')")
    public Page<InventoryTransferResponse> inventoryTransfers(@Argument Integer page,
                                                               @Argument Integer size) {
        log.debug("GQL inventoryTransfers");
        return transferService.getTransfers(
                PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
