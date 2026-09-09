package ecommerce.modules.seller.service;

import ecommerce.modules.seller.dto.request.SellerStatusRequest;
import ecommerce.modules.seller.dto.response.SellerResponse;

import java.util.UUID;

public interface SellerStatusService {

    SellerResponse suspendSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request);

    SellerResponse activateSeller(UUID sellerPublicId, UUID actorId);

    SellerResponse approveSeller(UUID sellerPublicId, UUID actorId);

    SellerResponse rejectSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request);

    SellerResponse blockSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request);

    SellerResponse closeSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request);
}
