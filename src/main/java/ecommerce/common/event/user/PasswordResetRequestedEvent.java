package ecommerce.common.event.user;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

/**
 * Published when a user requests a password reset.
 * Consumed to: send the reset link email. Token expires after a short window
 * enforced by the token validation service.
 */
public record PasswordResetRequestedEvent(
        UUID userId,
        String email,
        String fullName,
        String resetToken,
        int expiryMinutes
) implements DomainEvent {}
