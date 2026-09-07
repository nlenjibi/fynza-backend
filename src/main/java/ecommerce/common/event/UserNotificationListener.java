package ecommerce.common.event;

import ecommerce.common.event.user.PasswordResetRequestedEvent;
import ecommerce.common.event.user.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles user lifecycle notifications: welcome emails, verification links, password resets.
 * Runs AFTER_COMMIT — a failed email never rolls back account creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("[UserNotification] New registration — userId={}, email={}, role={}, verified={}",
                event.userId(), event.email(), event.role(), event.emailVerified());
        // TODO: if (!event.emailVerified()) send verification email with event.verificationToken()
        // TODO: notificationService.send(WELCOME, event.email(), vars(event))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("[UserNotification] Password reset requested — userId={}, email={}",
                event.userId(), event.email());
        // TODO: notificationService.send(PASSWORD_RESET, event.email(), vars(event))
    }
}
