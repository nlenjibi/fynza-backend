package ecommerce.modules.auth.service;

import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.RegisterRequest;
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
}
