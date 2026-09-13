package ecommerce.modules.store.validation;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.exception.StoreStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class StoreStatusTransitionValidator {

    private static final Map<StoreStatus, Set<StoreStatus>> ALLOWED = new EnumMap<>(StoreStatus.class);

    static {
        ALLOWED.put(StoreStatus.DRAFT,          Set.of(StoreStatus.PENDING_REVIEW));
        ALLOWED.put(StoreStatus.PENDING_REVIEW, Set.of(StoreStatus.ACTIVE, StoreStatus.DRAFT));
        ALLOWED.put(StoreStatus.ACTIVE,         Set.of(StoreStatus.PAUSED, StoreStatus.SUSPENDED, StoreStatus.CLOSED));
        ALLOWED.put(StoreStatus.PAUSED,         Set.of(StoreStatus.ACTIVE, StoreStatus.CLOSED));
        ALLOWED.put(StoreStatus.SUSPENDED,      Set.of(StoreStatus.ACTIVE, StoreStatus.CLOSED));
        ALLOWED.put(StoreStatus.CLOSED,         Set.of(StoreStatus.ARCHIVED));
        ALLOWED.put(StoreStatus.ARCHIVED,       Set.of());
    }

    public void validate(StoreStatus from, StoreStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new StoreStatusTransitionException(from, to);
        }
    }
}
