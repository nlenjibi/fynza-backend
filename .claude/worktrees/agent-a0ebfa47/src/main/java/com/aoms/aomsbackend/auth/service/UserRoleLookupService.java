package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.entity.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserRoleLookupService {
    List<UserRole> getRolesForUser(UUID userId);
}

