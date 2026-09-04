package com.aoms.aomsbackend.auth.service;


import java.util.UUID;
import com.aoms.aomsbackend.auth.entity.UserRoleType;

/**
 * The interface User role access service.
 */
public interface UserRoleAccessService {
    /**
     * Has access boolean.
     *
     * @param userId         the user id
     * @param organizationId the organization id
     * @param requiredRole   the required role
     * @return the boolean
     */
    boolean hasAccess(UUID userId, UUID organizationId, UserRoleType requiredRole);
}
