package ecommerce.modules.authz.service;

import ecommerce.common.enums.ScopeType;

import java.util.UUID;

public interface AuthorizationService {

    boolean hasPermission(UUID userId, String permissionCode);

    boolean hasPermission(UUID userId, String permissionCode, ScopeType scopeType, UUID scopeId);

    boolean hasScope(UUID userId, ScopeType scopeType, UUID scopeId);

    void authorize(UUID userId, String permissionCode);

    void authorize(UUID userId, String permissionCode, ScopeType scopeType, UUID scopeId);
}
