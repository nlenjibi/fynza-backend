package ecommerce.graphql.resolver.auth;

import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.*;
import ecommerce.graphql.input.*;
import ecommerce.modules.auth.dto.*;
import ecommerce.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthResolver {

    private final AuthService authService;

    // ── Core auth ─────────────────────────────────────────────────────────────

    @MutationMapping
    public AuthPayload register(@Argument RegisterInput input) {
        log.debug("GQL register email={}", input.getEmail());
        return toPayload(authService.register(RegisterRequest.builder()
                .email(input.getEmail())
                .password(input.getPassword())
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .phone(input.getPhone())
                .role(input.getRole())
                .build()));
    }

    @MutationMapping
    public AuthPayload login(@Argument LoginInput input) {
        log.debug("GQL login email={}", input.getEmail());
        return toPayload(authService.login(LoginRequest.builder()
                .email(input.getEmail())
                .password(input.getPassword())
                .build()));
    }

    @MutationMapping
    public AuthPayload refreshToken(@Argument RefreshTokenInput input) {
        log.debug("GQL refreshToken");
        return toPayload(authService.refreshToken(input.getRefreshToken()));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean logout(@Argument RefreshTokenInput input,
                          @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL logout user={}", principal.getId());
        authService.logout(principal.getId(), input.getRefreshToken());
        return true;
    }

    // ── Email verification ────────────────────────────────────────────────────

    @MutationMapping
    public boolean verifyEmail(@Argument VerifyEmailInput input) {
        log.debug("GQL verifyEmail");
        authService.verifyEmail(input.getToken());
        return true;
    }

    @MutationMapping
    public boolean resendVerification(@Argument ResendVerificationInput input) {
        log.debug("GQL resendVerification email={}", input.getEmail());
        authService.resendVerificationEmail(input.getEmail());
        return true;
    }

    // ── Password recovery ─────────────────────────────────────────────────────

    @MutationMapping
    public boolean forgotPassword(@Argument ForgotPasswordInput input) {
        log.debug("GQL forgotPassword email={}", input.getEmail());
        authService.forgotPassword(input.getEmail());
        return true;
    }

    @MutationMapping
    public boolean resetPassword(@Argument ResetPasswordInput input) {
        log.debug("GQL resetPassword");
        authService.resetPassword(input.getToken(), input.getNewPassword(), input.getConfirmPassword());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean changePassword(@Argument ChangePasswordInput input,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL changePassword user={}", principal.getId());
        authService.changePassword(principal.getId(),
                input.getCurrentPassword(), input.getNewPassword(), input.getConfirmPassword());
        return true;
    }

    // ── Session management ────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<SessionInfo> activeSessions(@Argument String currentRefreshToken,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL activeSessions user={}", principal.getId());
        return authService.listActiveSessions(principal.getId(), currentRefreshToken)
                .stream()
                .map(s -> SessionInfo.builder()
                        .sessionId(s.getSessionId())
                        .deviceName(s.getDeviceName())
                        .ipAddress(s.getIpAddress())
                        .createdAt(s.getCreatedAt())
                        .lastActivityAt(s.getLastActivityAt())
                        .expiresAt(s.getExpiresAt())
                        .current(s.isCurrent())
                        .build())
                .toList();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean revokeSession(@Argument RevokeSessionInput input,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL revokeSession sessionId={} user={}", input.getSessionId(), principal.getId());
        authService.revokeSession(principal.getId(), input.getSessionId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean revokeOtherSessions(@Argument RevokeOtherSessionsInput input,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL revokeOtherSessions user={}", principal.getId());
        authService.revokeAllOtherSessions(principal.getId(), input.getRefreshToken());
        return true;
    }

    // ── MFA ───────────────────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public MfaSetupPayload setupMfa(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL setupMfa user={}", principal.getId());
        MfaSetupResponse r = authService.setupMfa(principal.getId());
        return MfaSetupPayload.builder()
                .secret(r.getSecret())
                .qrCodeUri(r.getQrCodeUri())
                .manualEntryCode(r.getManualEntryCode())
                .build();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean enableMfa(@Argument MfaEnableInput input,
                             @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL enableMfa user={}", principal.getId());
        authService.enableMfa(principal.getId(), input.getTotpCode());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean disableMfa(@Argument MfaDisableInput input,
                              @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL disableMfa user={}", principal.getId());
        authService.disableMfa(principal.getId(), input.getTotpCode());
        return true;
    }

    @MutationMapping
    public AuthPayload verifyMfa(@Argument MfaVerifyInput input) {
        log.debug("GQL verifyMfa");
        return toPayload(authService.verifyMfa(input.getChallengeToken(), input.getTotpCode()));
    }

    // ── Social / OAuth2 accounts ──────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<LinkedAccount> linkedAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL linkedAccounts user={}", principal.getId());
        return authService.listLinkedIdentities(principal.getId())
                .stream()
                .map(li -> LinkedAccount.builder()
                        .provider(li.getProvider())
                        .displayName(li.getDisplayName())
                        .email(li.getEmail())
                        .avatarUrl(li.getAvatarUrl())
                        .linkedAt(li.getLinkedAt())
                        .build())
                .toList();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean unlinkSocialAccount(@Argument String provider,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL unlinkSocialAccount provider={} user={}", provider, principal.getId());
        authService.unlinkSocialAccount(principal.getId(), provider.toLowerCase());
        return true;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private AuthPayload toPayload(AuthResponse r) {
        if (r.isMfaRequired()) {
            return AuthPayload.builder()
                    .mfaRequired(true)
                    .mfaChallengeToken(r.getMfaChallengeToken())
                    .build();
        }

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
                .mfaRequired(false)
                .build();
    }
}
