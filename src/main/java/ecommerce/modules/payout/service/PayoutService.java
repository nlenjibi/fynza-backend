package ecommerce.modules.payout.service;

import ecommerce.modules.payout.dto.response.PayoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PayoutService {

    PayoutResponse requestPayout(Long sellerId, UUID payoutAccountPublicId, BigDecimal amount, String idempotencyKey);

    PayoutResponse cancelPayout(Long sellerId, UUID payoutPublicId);

    Page<PayoutResponse> listPayouts(Long sellerId, Pageable pageable);

    Optional<PayoutResponse> findPayout(Long sellerId, UUID payoutPublicId);
}
