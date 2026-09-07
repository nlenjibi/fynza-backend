package ecommerce.modules.auth.service;

import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.LinkedIdentityResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.MfaSetupResponse;
import ecommerce.modules.auth.dto.RegisterRequest;
import ecommerce.modules.auth.dto.SessionResponse;
import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(UUID userId, String token);

    void cleanupExpiredSessions();

    AuthResponse oauth2Login(ecommerce.modules.user.entity.User user, jakarta.servlet.http.HttpServletRequest request);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword, String confirmPassword);

    void changePassword(UUID userId, String currentPassword, String newPassword, String confirmPassword);

    List<SessionResponse> listActiveSessions(UUID userId, String currentRefreshToken);

    void revokeSession(UUID userId, UUID sessionId);

    void revokeAllOtherSessions(UUID userId, String currentRefreshToken);

    MfaSetupResponse setupMfa(UUID userId);

    void enableMfa(UUID userId, String totpCode);

    void disableMfa(UUID userId, String totpCode);

    AuthResponse verifyMfa(String challengeToken, String totpCode);

    List<LinkedIdentityResponse> listLinkedIdentities(UUID userId);

    void unlinkSocialAccount(UUID userId, String provider);
}
