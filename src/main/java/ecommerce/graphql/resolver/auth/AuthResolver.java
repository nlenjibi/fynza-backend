package ecommerce.graphql.resolver.auth;

import ecommerce.graphql.dto.AuthPayload;
import ecommerce.graphql.dto.AuthUser;
import ecommerce.graphql.input.LoginInput;
import ecommerce.graphql.input.RefreshTokenInput;
import ecommerce.graphql.input.RegisterInput;
import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.RegisterRequest;
import ecommerce.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthResolver {

    private final AuthService authService;

    // ── Public mutations ───────────────────────────────────────────────────────

    @MutationMapping
    public AuthPayload login(@Argument LoginInput input) {
        log.info("GQL login(email={})", input.getEmail());
        AuthResponse response = authService.login(
                LoginRequest.builder()
                        .email(input.getEmail())
                        .password(input.getPassword())
                        .build()
        );
        return toPayload(response);
    }

    @MutationMapping
    public AuthPayload register(@Argument RegisterInput input) {
        log.info("GQL register(email={})", input.getEmail());
        AuthResponse response = authService.register(
                RegisterRequest.builder()
                        .email(input.getEmail())
                        .password(input.getPassword())
                        .firstName(input.getFirstName())
                        .lastName(input.getLastName())
                        .phone(input.getPhone())
                        .role(input.getRole())
                        .build()
        );
        return toPayload(response);
    }

    @MutationMapping
    public AuthPayload refreshToken(@Argument RefreshTokenInput input) {
        log.info("GQL refreshToken");
        return toPayload(authService.refreshToken(input.getRefreshToken()));
    }

    // ── Authenticated mutations ────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean logout(@Argument RefreshTokenInput input,
                          @ContextValue UUID userId) {
        log.info("GQL logout(user={})", userId);
        authService.logout(userId, input.getRefreshToken());
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthPayload toPayload(AuthResponse r) {
        AuthResponse.UserInfo info = r.getUser();
        AuthUser user = AuthUser.builder()
                .id(r.getUserId())
                .email(r.getEmail())
                .username(info != null ? info.getUsername() : r.getEmail())
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .role(r.getRole())
                .build();

        return AuthPayload.builder()
                .accessToken(r.getAccessToken())
                .refreshToken(r.getRefreshToken())
                .tokenType(r.getTokenType() != null ? r.getTokenType() : "Bearer")
                .expiresIn(r.getExpiresIn())
                .user(user)
                .build();
    }
}
