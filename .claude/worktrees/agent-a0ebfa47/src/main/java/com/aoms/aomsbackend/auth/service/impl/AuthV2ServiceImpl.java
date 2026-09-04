package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.dto.LoginRequest;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.auth.service.AuthV2Service;
import com.aoms.aomsbackend.common.exception.AccountInactiveException;
import com.aoms.aomsbackend.common.exception.InvalidCredentialsException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthV2ServiceImpl implements AuthV2Service {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    @Override
    public UserContextResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(InvalidCredentialsException::new);

        validateCredentials(user, loginRequest.getPassword());

        List<UserRole> userRoles = userRoleRepository.findByUserIdAndDeletedAtIsNull(user.getId());
        List<String> roles = userRoles.stream()
            .map(role -> "ROLE_" + role.getRole().name())
            .toList();

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionAttribute.V2_USER_ID.getKey(), user.getId().toString());
        session.setAttribute(SessionAttribute.V2_EMAIL.getKey(), user.getEmail());
        session.setAttribute(SessionAttribute.V2_ROLES.getKey(), roles);
        session.setAttribute(SessionAttribute.FIRST_NAME.getKey(), user.getFirstName());
        session.setAttribute(SessionAttribute.LAST_NAME.getKey(), user.getLastName());
        session.setMaxInactiveInterval((int) authProperties.getSessionExpirySeconds());

        log.info("V2 login successful for userId={}", user.getId());

        return UserContextResponse.builder()
            .userId(user.getId().toString())
            .email(user.getEmail())
            .roles(roles)
            .build();
    }

    @Override
    public UserProfileResponse getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionAttribute.V2_USER_ID.getKey()) == null) {
            throw new SessionExpiredException();
        }

        String userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());

        return UserProfileResponse.builder()
            .userId(userId)
            .email((String) session.getAttribute(SessionAttribute.V2_EMAIL.getKey()))
            .roles(resolveRoles(session))
            .firstName((String) session.getAttribute(SessionAttribute.FIRST_NAME.getKey()))
            .lastName((String) session.getAttribute(SessionAttribute.LAST_NAME.getKey()))
            .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("V2 logout for userId={}", session.getAttribute(SessionAttribute.V2_USER_ID.getKey()));
            session.invalidate();
        }
        clearSessionCookie(response);
    }

    @Override
    public UserContextResponse validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionAttribute.V2_USER_ID.getKey()) == null) {
            throw new SessionExpiredException();
        }

        String userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());

        return UserContextResponse.builder()
            .userId(userId)
            .email((String) session.getAttribute(SessionAttribute.V2_EMAIL.getKey()))
            .roles(resolveRoles(session))
            .build();
    }

    private void validateCredentials(User user, String rawPassword) {
        if (!user.isActive()) {
            throw new AccountInactiveException();
        }
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveRoles(HttpSession session) {
        return (List<String>) session.getAttribute(SessionAttribute.V2_ROLES.getKey());
    }

    private void clearSessionCookie(HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("SESSION", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }
}
