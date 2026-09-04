package ecommerce.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginAttemptService Unit Tests
 * 
 * Tests login attempt tracking functionality:
 * - Track failed login attempts per email/IP
 * - Account lockout after 5 failed attempts
 * - 15-minute lockout duration
 * 
 * Per system.md v2.1:
 * - 5 failed login attempts triggers 15-minute lockout
 * - Both per-email and per-IP lockout supported
 * - Uses Caffeine cache with 15-minute expiry
 */
@DisplayName("LoginAttemptService Tests")
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Nested
    @DisplayName("Failed Login Attempts")
    class FailedAttemptsTests {

        @Test
        @DisplayName("Should record failed attempt")
        void loginFailed_IncrementsCount() {
            // Arrange
            String identifier = "test@example.com";

            // Act
            loginAttemptService.loginFailed(identifier);

            // Assert
            assertEquals(1, loginAttemptService.getAttempts(identifier));
        }

        @Test
        @DisplayName("Should increment failed attempts")
        void loginFailed_MultipleTimes_IncrementsCount() {
            // Arrange
            String identifier = "test@example.com";

            // Act
            loginAttemptService.loginFailed(identifier);
            loginAttemptService.loginFailed(identifier);
            loginAttemptService.loginFailed(identifier);

            // Assert
            assertEquals(3, loginAttemptService.getAttempts(identifier));
        }

        @Test
        @DisplayName("Should return zero for new identifier")
        void getAttempts_NewIdentifier_ReturnsZero() {
            // Arrange
            String identifier = "newuser@example.com";

            // Act
            int attempts = loginAttemptService.getAttempts(identifier);

            // Assert
            assertEquals(0, attempts);
        }
    }

    @Nested
    @DisplayName("Account Lockout")
    class LockoutTests {

        @Test
        @DisplayName("Should lock account after 5 failed attempts")
        void isLocked_AfterMaxAttempts_ReturnsTrue() {
            // Arrange
            String identifier = "test@example.com";

            // Act
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                loginAttemptService.loginFailed(identifier);
            }

            // Assert
            assertTrue(loginAttemptService.isLocked(identifier));
        }

        @Test
        @DisplayName("Should not lock account before max attempts")
        void isLocked_BeforeMaxAttempts_ReturnsFalse() {
            // Arrange
            String identifier = "test@example.com";

            // Act
            for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
                loginAttemptService.loginFailed(identifier);
            }

            // Assert
            assertFalse(loginAttemptService.isLocked(identifier));
        }

        @Test
        @DisplayName("Should clear failed attempts after successful login")
        void loginSucceeded_ClearsFailedAttempts() {
            // Arrange
            String identifier = "test@example.com";
            for (int i = 0; i < 3; i++) {
                loginAttemptService.loginFailed(identifier);
            }

            // Act
            loginAttemptService.loginSucceeded(identifier);

            // Assert
            assertEquals(0, loginAttemptService.getAttempts(identifier));
            assertFalse(loginAttemptService.isLocked(identifier));
        }

        @Test
        @DisplayName("Should return full lockout duration when locked")
        void getRemainingLockoutMinutes_WhenLocked_Returns15() {
            // Arrange
            String identifier = "test@example.com";
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                loginAttemptService.loginFailed(identifier);
            }

            // Act
            int remainingMinutes = loginAttemptService.getRemainingLockoutMinutes(identifier);

            // Assert
            assertEquals(LOCKOUT_MINUTES, remainingMinutes);
        }

        @Test
        @DisplayName("Should return 0 when not locked")
        void getRemainingLockoutMinutes_WhenNotLocked_Returns0() {
            // Arrange
            String identifier = "test@example.com";

            // Act
            int remainingMinutes = loginAttemptService.getRemainingLockoutMinutes(identifier);

            // Assert
            assertEquals(0, remainingMinutes);
        }
    }

    @Nested
    @DisplayName("IP-based Lockout")
    class IPLockoutTests {

        @Test
        @DisplayName("Should track attempts per IP")
        void loginFailed_ByIP_TracksCorrectly() {
            // Arrange
            String ipAddress = "192.168.1.100";

            // Act
            loginAttemptService.loginFailed(ipAddress);
            loginAttemptService.loginFailed(ipAddress);

            // Assert
            assertEquals(2, loginAttemptService.getAttempts(ipAddress));
        }

        @Test
        @DisplayName("Should lock IP after max attempts")
        void isLocked_ByIP_AfterMaxAttempts_ReturnsTrue() {
            // Arrange
            String ipAddress = "192.168.1.100";

            // Act
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                loginAttemptService.loginFailed(ipAddress);
            }

            // Assert
            assertTrue(loginAttemptService.isLocked(ipAddress));
        }

        @Test
        @DisplayName("Email and IP lockouts should be independent")
        void isLocked_EmailAndIP_IndependentLockouts() {
            // Arrange
            String email = "test@example.com";
            String ipAddress = "192.168.1.100";

            // Lock email
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                loginAttemptService.loginFailed(email);
            }

            // Only 1 attempt from IP
            loginAttemptService.loginFailed(ipAddress);

            // Assert - email locked, IP not locked
            assertTrue(loginAttemptService.isLocked(email));
            assertFalse(loginAttemptService.isLocked(ipAddress));
        }
    }

    @Nested
    @DisplayName("Clear Attempts")
    class ClearAttemptsTests {

        @Test
        @DisplayName("Should clear all attempts")
        void clearAll_ClearsAllAttempts() {
            // Arrange
            String identifier = "test@example.com";
            loginAttemptService.loginFailed(identifier);
            loginAttemptService.loginFailed(identifier);

            // Act
            loginAttemptService.clearAll();

            // Assert
            assertEquals(0, loginAttemptService.getAttempts(identifier));
            assertFalse(loginAttemptService.isLocked(identifier));
        }
    }
}
