package ecommerce.modules.seller.service;

import ecommerce.modules.seller.dto.request.SellerSearchRequest;
import ecommerce.modules.seller.dto.request.UpdateSellerRequest;
import ecommerce.modules.seller.dto.response.SellerDetailResponse;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.dto.response.SellerStatusHistoryResponse;
import ecommerce.modules.seller.dto.response.SellerSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SellerService {

    SellerResponse provision(UUID ownerUserId, String displayName);

    SellerDetailResponse getMySeller(UUID userId);

    SellerDetailResponse getSellerByPublicId(UUID publicId);

    SellerResponse updateMySeller(UUID userId, UpdateSellerRequest request);

    Page<SellerSummaryResponse> searchSellers(SellerSearchRequest params, Pageable pageable);

    List<SellerStatusHistoryResponse> getStatusHistory(UUID sellerPublicId);
}
