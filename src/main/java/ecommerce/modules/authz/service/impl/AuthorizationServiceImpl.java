package ecommerce.modules.authz.service.impl;

import ecommerce.common.enums.ScopeType;
import ecommerce.modules.authz.repository.UserRoleEntityRepository;
import ecommerce.modules.authz.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRoleEntityRepository userRoleEntityRepository;

    @Override
    public boolean hasPermission(UUID userId, String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permissionCode::equals);
    }

    @Override
    public boolean hasPermission(UUID userId, String permissionCode, ScopeType scopeType, UUID scopeId) {
        if (!hasPermission(userId, permissionCode)) return false;
        return userRoleEntityRepository
                .findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, scopeType, scopeId)
                .stream()
                .anyMatch(ur -> ur.getExpiresAt() == null || ur.getExpiresAt().isAfter(Instant.now()));
    }

    @Override
    public boolean hasScope(UUID userId, ScopeType scopeType, UUID scopeId) {
        return !userRoleEntityRepository
                .findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, scopeType, scopeId)
                .isEmpty();
    }

    @Override
    public void authorize(UUID userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            throw new AccessDeniedException("Access denied: missing permission '" + permissionCode + "'");
        }
    }

    @Override
    public void authorize(UUID userId, String permissionCode, ScopeType scopeType, UUID scopeId) {
        if (!hasPermission(userId, permissionCode, scopeType, scopeId)) {
            throw new AccessDeniedException("Access denied: missing scoped permission '" + permissionCode + "'");
        }
    }
}
