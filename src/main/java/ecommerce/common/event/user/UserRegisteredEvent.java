package ecommerce.common.event.user;

import ecommerce.common.event.DomainEvent;
import ecommerce.common.enums.Role;

import java.util.UUID;

/**
 * Published after a new user account is persisted (email/password or OAuth).
 * Consumed to: send welcome email, trigger email verification flow if needed.
 */
public record UserRegisteredEvent(
        Long userId,
        UUID publicUserId,
        String email,
        String fullName,
        Role role,
        boolean emailVerified,
        String verificationToken
) implements DomainEvent {}
