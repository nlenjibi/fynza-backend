package ecommerce.modules.payout.event;

import ecommerce.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutCompletedEvent(
        Long payoutId,
        UUID payoutPublicId,
        Long sellerId,
        String payoutNumber,
        BigDecimal amount,
        String currency
) implements DomainEvent {}
