package ecommerce.graphql.resolver.auth;

import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.*;
import ecommerce.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Auth GraphQL resolver — queries only.
 * All auth mutations (login, register, refresh, logout, MFA, password, sessions)
 * are handled exclusively by REST: POST /v1/auth/*
 * Per PRD §20-21: authentication is REST-only; GraphQL consumes the identity.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthResolver {

    private final AuthService authService;

    // ── Session queries (authenticated) ──────────────────────────────────────

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

    // ── Linked OAuth2 accounts (authenticated) ────────────────────────────────

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
}
