package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import com.aoms.aomsbackend.auth.service.impl.AuthServiceImpl;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;
    @Mock private AuthProperties authProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(authProperties.getSessionExpirySeconds()).thenReturn(3600L);

        // Note: Using null for dependencies that are not needed for these tests
        // In real scenarios, these would be mocked or injected
        authService = new AuthServiceImpl(
                authProperties,
                null, // UserRepository
                null, // UserRoleRepository
                null  // SsoIntegrationService
        );
    }

    @Test
    void getCurrentUser_validSession_returnsUserContext() {
        UUID userId = UUID.randomUUID();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(userId.toString());
        when(session.getAttribute(SessionAttribute.EMAIL.getKey())).thenReturn("user@example.com");
        when(session.getAttribute(SessionAttribute.ROLES.getKey())).thenReturn(List.of("ROLE_EMPLOYEE"));
        when(session.getAttribute(SessionAttribute.DEPARTMENT.getKey())).thenReturn("Engineering");
        when(session.getAttribute(SessionAttribute.POSITION.getKey())).thenReturn("Engineer");

        UserProfileResponse result = authService.getCurrentUser(request);

        assertThat(result.getUserId()).isEqualTo(userId.toString());
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRoles()).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void getCurrentUser_noSession_throwsSessionExpiredException() {
        when(request.getSession(false)).thenReturn(null);

        assertThatThrownBy(() -> authService.getCurrentUser(request))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void getCurrentUser_sessionMissingUserId_throwsSessionExpiredException() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(null);

        assertThatThrownBy(() -> authService.getCurrentUser(request))
            .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void validateSession_validSession_returnsUserContext() {
        UUID userId = UUID.randomUUID();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(userId.toString());
        when(session.getAttribute(SessionAttribute.EMAIL.getKey())).thenReturn("user@example.com");
        when(session.getAttribute(SessionAttribute.ROLES.getKey())).thenReturn(List.of("ROLE_MANAGER"));
        when(session.getAttribute(SessionAttribute.DEPARTMENT.getKey())).thenReturn(null);
        when(session.getAttribute(SessionAttribute.POSITION.getKey())).thenReturn(null);

        UserContextResponse result = authService.validateSession(request);

        assertThat(result.getRoles()).containsExactly("ROLE_MANAGER");
    }

    @Test
    void validateSession_expiredSession_throwsSessionExpiredException() {
        when(request.getSession(false)).thenReturn(null);

        assertThatThrownBy(() -> authService.validateSession(request))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void logout_validSession_invalidatesSession() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.EMAIL.getKey())).thenReturn("user@example.com");

        authService.logout(request);

        verify(session).invalidate();
    }

    @Test
    void logout_noSession_doesNothing() {
        when(request.getSession(false)).thenReturn(null);

        authService.logout(request);

        // No session to invalidate, should complete without error
    }
}