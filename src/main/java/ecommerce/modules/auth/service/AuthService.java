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

    /**
     * Handle OAuth2 login - generate tokens for OAuth2 authenticated users
     * @param user The authenticated user
     * @param request The HTTP request
     * @return AuthResponse with tokens
     */
    AuthResponse oauth2Login(ecommerce.modules.user.entity.User user, jakarta.servlet.http.HttpServletRequest request);
}
