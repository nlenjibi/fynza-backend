package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.auth.service.UserRoleLookupService;
import com.aoms.aomsbackend.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleLookupServiceImpl implements UserRoleLookupService {

    private final UserRoleRepository userRoleRepository;

    @Override
    @Cacheable(cacheNames = RedisConfig.CACHE_USER_ROLES, key = "#userId")
    public List<UserRole> getRolesForUser(UUID userId) {
        return userRoleRepository.findByUserIdAndDeletedAtIsNull(userId);
    }
}

