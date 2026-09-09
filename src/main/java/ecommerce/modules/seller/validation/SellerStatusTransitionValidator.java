package ecommerce.modules.seller.validation;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.exception.SellerStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class SellerStatusTransitionValidator {

    private static final Map<SellerStatus, Set<SellerStatus>> ALLOWED = new EnumMap<>(SellerStatus.class);

    static {
        ALLOWED.put(SellerStatus.DRAFT,                EnumSet.of(SellerStatus.PENDING_VERIFICATION));
        ALLOWED.put(SellerStatus.PENDING_VERIFICATION, EnumSet.of(SellerStatus.UNDER_REVIEW));
        ALLOWED.put(SellerStatus.UNDER_REVIEW,         EnumSet.of(SellerStatus.ACTIVE, SellerStatus.REJECTED));
        ALLOWED.put(SellerStatus.ACTIVE,               EnumSet.of(SellerStatus.SUSPENDED, SellerStatus.BLOCKED, SellerStatus.CLOSED));
        ALLOWED.put(SellerStatus.SUSPENDED,            EnumSet.of(SellerStatus.ACTIVE, SellerStatus.BLOCKED));
        ALLOWED.put(SellerStatus.REJECTED,             EnumSet.noneOf(SellerStatus.class));
        ALLOWED.put(SellerStatus.BLOCKED,              EnumSet.noneOf(SellerStatus.class));
        ALLOWED.put(SellerStatus.CLOSED,               EnumSet.noneOf(SellerStatus.class));
        ALLOWED.put(SellerStatus.PENDING,              EnumSet.of(SellerStatus.ACTIVE, SellerStatus.SUSPENDED));
    }

    public void validate(SellerStatus from, SellerStatus to) {
        Set<SellerStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(SellerStatus.class));
        if (!allowed.contains(to)) {
            throw new SellerStatusTransitionException(from, to);
        }
    }
}
