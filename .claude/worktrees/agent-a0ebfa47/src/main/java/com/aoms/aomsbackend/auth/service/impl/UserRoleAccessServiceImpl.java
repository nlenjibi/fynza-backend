package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.auth.service.UserRoleLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.aoms.aomsbackend.auth.entity.UserRole;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleAccessServiceImpl implements UserRoleAccessService {

    private final UserRoleLookupService userRoleLookupService;

    @Override
    public boolean hasAccess(UUID userId, UUID organizationId, UserRoleType requiredRole) {
        List<UserRole> roles = userRoleLookupService.getRolesForUser(userId);
        List<UserRole> locationMatched = filterByLocation(roles, organizationId);
        return locationMatched.stream().anyMatch(role -> hasRequiredRole(role.getRole(), requiredRole));
    }

    private List<UserRole> filterByLocation(List<UserRole> roles, UUID organizationId) {
        if (organizationId == null) {
            return roles;
        }
        return roles.stream()
                .filter(role -> role.getOrganisationId() == null || role.getOrganisationId().equals(organizationId))
                .toList();
    }


    private boolean hasRequiredRole(UserRoleType actualRole, UserRoleType requiredRole) {
        int actualRank = actualRole != null ? actualRole.getRank() : 0;
        int requiredRank = requiredRole != null ? requiredRole.getRank() : 0;
        return actualRank >= requiredRank;
    }
}
