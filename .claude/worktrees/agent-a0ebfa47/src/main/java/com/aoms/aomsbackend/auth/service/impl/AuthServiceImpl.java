package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.dto.SSOUserProfile;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.auth.service.AuthService;
import com.aoms.aomsbackend.auth.service.SsoIntegrationService;
import com.aoms.aomsbackend.common.exception.TokenVerificationException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final SsoIntegrationService ssoIntegrationService;

    @Override
    public UserContextResponse handleV2Callback(String token, HttpServletRequest request) {
        if (token == null || token.isBlank()) {
            throw new TokenVerificationException();
        }

        SSOUserProfile profile = ssoIntegrationService.fetchProfile(token);

        if (profile.isDeleted()) {
            log.warn("SSO profile is marked deleted for userId={}", profile.getUserId());
            throw new TokenVerificationException();
        }

        User user = resolveOrCreateUser(profile);

        List<UserRole> userRoles = userRoleRepository.findByUserIdAndDeletedAtIsNull(user.getId());
        List<String> roles = userRoles.isEmpty()
            ? List.of(UserRoleType.EMPLOYEE.name())
            : userRoles.stream().map(ur -> ur.getRole().name()).collect(Collectors.toList());

        HttpSession session = request.getSession(true);
        session.setAttribute(SessionAttribute.ACCESS_TOKEN.getKey(), token);
        session.setAttribute(SessionAttribute.USER_ID.getKey(), user.getId().toString());
        session.setAttribute(SessionAttribute.EMAIL.getKey(), user.getEmail());
        session.setAttribute(SessionAttribute.ROLES.getKey(), roles);
        session.setAttribute(SessionAttribute.ARMS_USER_ID.getKey(), profile.getUserId());
        session.setAttribute(SessionAttribute.FIRST_NAME.getKey(), profile.getFirstName());
        session.setAttribute(SessionAttribute.LAST_NAME.getKey(), profile.getLastName());
        session.setAttribute(SessionAttribute.OTHER_NAME.getKey(), profile.getOtherName());
        session.setAttribute(SessionAttribute.PROFILE_IMAGE.getKey(), profile.getProfileImage());
        session.setAttribute(SessionAttribute.OFFICE.getKey(), profile.getOffice());
        session.setAttribute(SessionAttribute.ORGANIZATION.getKey(), profile.getOrganization());
        session.setMaxInactiveInterval((int) authProperties.getSessionExpirySeconds());

        log.info("SSO login successful for ssoUserId={}, userId={}, roles={}", profile.getUserId(), user.getId(), roles);

        return UserContextResponse.builder()
            .userId(user.getId().toString())
            .email(user.getEmail())
            .roles(roles)
            .build();
    }

    @Override
    public UserProfileResponse getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException();
        }

        String userId = resolveUserId(session);
        if (userId == null) {
            throw new SessionExpiredException();
        }

        return buildFullProfile(session, userId);
    }

    @Override
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("Invalidating session for user: {}", session.getAttribute(SessionAttribute.EMAIL.getKey()));
            session.invalidate();
        }
    }

    @Override
    public UserContextResponse validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException();
        }

        String userId = resolveUserId(session);
        if (userId == null) {
            throw new SessionExpiredException();
        }

        return buildSlimContext(session, userId);
    }

    private User resolveOrCreateUser(SSOUserProfile profile) {
        return userRepository.findBySsoUserId(profile.getUserId())
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .ssoUserId(profile.getUserId())
                    .email(profile.getEmail())
                    .firstName(profile.getFirstName() != null ? profile.getFirstName() : "")
                    .lastName(profile.getLastName() != null ? profile.getLastName() : "")
                    .isActive(true)
                    .build()
            ));
    }

    private String resolveUserId(HttpSession session) {
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveRoles(HttpSession session) {
        List<String> roles = (List<String>) session.getAttribute(SessionAttribute.ROLES.getKey());
        if (roles == null) {
            roles = (List<String>) session.getAttribute(SessionAttribute.V2_ROLES.getKey());
        }
        return roles;
    }

    private String resolveEmail(HttpSession session) {
        String email = (String) session.getAttribute(SessionAttribute.EMAIL.getKey());
        if (email == null) {
            email = (String) session.getAttribute(SessionAttribute.V2_EMAIL.getKey());
        }
        return email;
    }

    private UserContextResponse buildSlimContext(HttpSession session, String userId) {
        return UserContextResponse.builder()
            .userId(userId)
            .email(resolveEmail(session))
            .roles(resolveRoles(session))
            .build();
    }

    private UserProfileResponse buildFullProfile(HttpSession session, String userId) {
        return UserProfileResponse.builder()
            .userId(userId)
            .email(resolveEmail(session))
            .roles(resolveRoles(session))
            .firstName((String) session.getAttribute(SessionAttribute.FIRST_NAME.getKey()))
            .lastName((String) session.getAttribute(SessionAttribute.LAST_NAME.getKey()))
            .otherName((String) session.getAttribute(SessionAttribute.OTHER_NAME.getKey()))
            .profileImage((String) session.getAttribute(SessionAttribute.PROFILE_IMAGE.getKey()))
            .office((String) session.getAttribute(SessionAttribute.OFFICE.getKey()))
            .organization((String) session.getAttribute(SessionAttribute.ORGANIZATION.getKey()))
            .build();
    }
}
