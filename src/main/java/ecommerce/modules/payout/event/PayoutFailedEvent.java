package ecommerce.modules.payout.event;

import ecommerce.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutFailedEvent(
        Long payoutId,
        UUID payoutPublicId,
        Long sellerId,
        String payoutNumber,
        BigDecimal amount,
        String currency,
        String reason
) implements DomainEvent {}
