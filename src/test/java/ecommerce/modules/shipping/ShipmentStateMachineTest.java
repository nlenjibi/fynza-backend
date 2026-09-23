package ecommerce.modules.shipping;

import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.exception.InvalidShipmentTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentStateMachineTest {

    @Nested
    class ValidTransitions {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "DRAFT,            READY",
                "DRAFT,            CANCELLED",
                "READY,            LABEL_CREATED",
                "READY,            CANCELLED",
                "LABEL_CREATED,    PICKUP_SCHEDULED",
                "LABEL_CREATED,    PICKED_UP",
                "LABEL_CREATED,    CANCELLED",
                "PICKUP_SCHEDULED, PICKED_UP",
                "PICKUP_SCHEDULED, CANCELLED",
                "PICKED_UP,        IN_TRANSIT",
                "IN_TRANSIT,       OUT_FOR_DELIVERY",
                "IN_TRANSIT,       DELIVERY_FAILED",
                "IN_TRANSIT,       LOST",
                "IN_TRANSIT,       DAMAGED",
                "OUT_FOR_DELIVERY, DELIVERED",
                "OUT_FOR_DELIVERY, DELIVERY_FAILED",
                "DELIVERY_FAILED,  OUT_FOR_DELIVERY",
                "DELIVERY_FAILED,  RETURN_TO_SENDER",
                "RETURN_TO_SENDER, RETURNED"
        })
        void validate_doesNotThrow_forAllowedTransition(ShipmentStatus from, ShipmentStatus to) {
            ShipmentStateMachine.validate(from, to);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "DRAFT,           READY",
                "IN_TRANSIT,      DELIVERY_FAILED",
                "DELIVERY_FAILED, RETURN_TO_SENDER"
        })
        void canTransition_returnsTrue_forAllowedTransition(ShipmentStatus from, ShipmentStatus to) {
            assertThat(ShipmentStateMachine.canTransition(from, to)).isTrue();
        }
    }

    @Nested
    class InvalidTransitions {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "DRAFT,      IN_TRANSIT",
                "DRAFT,      DELIVERED",
                "READY,      PICKED_UP",
                "PICKED_UP,  DELIVERED",
                "IN_TRANSIT, CANCELLED",
                "DELIVERED,  IN_TRANSIT",
                "CANCELLED,  READY",
                "RETURNED,   READY",
                "LOST,       IN_TRANSIT",
                "DAMAGED,    DELIVERED"
        })
        void validate_throwsInvalidShipmentTransitionException_forDisallowedTransition(
                ShipmentStatus from, ShipmentStatus to) {
            assertThatThrownBy(() -> ShipmentStateMachine.validate(from, to))
                    .isInstanceOf(InvalidShipmentTransitionException.class)
                    .hasMessageContaining(from.name())
                    .hasMessageContaining(to.name());
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "DRAFT,     IN_TRANSIT",
                "DELIVERED, IN_TRANSIT",
                "LOST,      IN_TRANSIT"
        })
        void canTransition_returnsFalse_forDisallowedTransition(ShipmentStatus from, ShipmentStatus to) {
            assertThat(ShipmentStateMachine.canTransition(from, to)).isFalse();
        }
    }

    @Nested
    class TerminalStates {

        @ParameterizedTest(name = "{0} is terminal")
        @EnumSource(value = ShipmentStatus.class, names = {"DELIVERED", "RETURNED", "CANCELLED", "LOST", "DAMAGED"})
        void validate_throwsForAllTargets_fromTerminalState(ShipmentStatus terminalStatus) {
            for (ShipmentStatus target : ShipmentStatus.values()) {
                assertThatThrownBy(() -> ShipmentStateMachine.validate(terminalStatus, target))
                        .isInstanceOf(InvalidShipmentTransitionException.class);
            }
        }

        @ParameterizedTest(name = "canTransition returns false for all targets from {0}")
        @EnumSource(value = ShipmentStatus.class, names = {"DELIVERED", "RETURNED", "CANCELLED", "LOST", "DAMAGED"})
        void canTransition_returnsFalse_forAllTargetsFromTerminalState(ShipmentStatus terminalStatus) {
            for (ShipmentStatus target : ShipmentStatus.values()) {
                assertThat(ShipmentStateMachine.canTransition(terminalStatus, target))
                        .as("expected no transition from %s to %s", terminalStatus, target)
                        .isFalse();
            }
        }
    }
}
