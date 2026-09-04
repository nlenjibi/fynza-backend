package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.dto.LoginRequest;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.auth.service.impl.AuthV2ServiceImpl;
import com.aoms.aomsbackend.common.exception.AccountInactiveException;
import com.aoms.aomsbackend.common.exception.InvalidCredentialsException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthV2ServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpServletResponse httpResponse;
    @Mock private HttpSession session;

    private AuthV2ServiceImpl authV2Service;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        AuthProperties props = new AuthProperties();
        props.setSessionExpirySeconds(3600);
        authV2Service = new AuthV2ServiceImpl(
            userRepository, userRoleRepository, passwordEncoder, props
        );
    }

    @Test
    void login_validCredentials_createsSessionAndReturnsContext() {
        String rawPassword = "secret123";
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .password(passwordEncoder.encode(rawPassword))
            .isActive(true)
            .build();

        UserRole employeeRole = UserRole.builder().role(UserRoleType.EMPLOYEE).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserIdAndDeletedAtIsNull(user.getId()))
            .thenReturn(List.of(employeeRole));
        when(httpRequest.getSession(true)).thenReturn(session);

        UserContextResponse result = authV2Service.login(
            new LoginRequest("user@example.com", rawPassword), httpRequest
        );

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRoles()).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .password(passwordEncoder.encode("correct-password"))
            .isActive(true)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authV2Service.login(
            new LoginRequest("user@example.com", "wrong-password"), httpRequest
        )).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_userNotFound_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authV2Service.login(
            new LoginRequest("ghost@example.com", "any-password"), httpRequest
        )).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_inactiveUser_throwsAccountInactiveException() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("inactive@example.com")
            .password(passwordEncoder.encode("password"))
            .isActive(false)
            .build();

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authV2Service.login(
            new LoginRequest("inactive@example.com", "password"), httpRequest
        )).isInstanceOf(AccountInactiveException.class);
    }

    @Test
    void getCurrentUser_noSession_throwsSessionExpiredException() {
        when(httpRequest.getSession(false)).thenReturn(null);

        assertThatThrownBy(() -> authV2Service.getCurrentUser(httpRequest))
            .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void logout_validSession_invalidatesAndClearsCookie() {
        when(httpRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.V2_USER_ID.getKey()))
            .thenReturn(UUID.randomUUID().toString());

        authV2Service.logout(httpRequest, httpResponse);

        verify(session).invalidate();
        verify(httpResponse).addCookie(any());
    }
}
