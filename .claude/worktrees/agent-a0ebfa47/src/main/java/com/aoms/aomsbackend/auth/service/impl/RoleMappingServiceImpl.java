package com.aoms.aomsbackend.auth.service.impl;

import com.aoms.aomsbackend.auth.dto.ArmProfile;
import com.aoms.aomsbackend.auth.service.RoleMappingService;
import com.aoms.aomsbackend.config.ArmProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleMappingServiceImpl implements RoleMappingService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final ArmProperties armProperties;


    @Override
    public Set<GrantedAuthority> mapToAuthorities(ArmProfile profile) {
        if (profile.isEmpty()) {
            return Set.of(new SimpleGrantedAuthority(DEFAULT_ROLE));
        }

        Map<String, String> mappings = armProperties.getRoleMappings();

        String role = resolveRole(profile, mappings);
        return Set.of(new SimpleGrantedAuthority(role));
    }

    private String resolveRole(ArmProfile profile, Map<String, String> mappings) {
        if (profile.getDepartment() != null) {
            String mapped = mappings.get(profile.getDepartment());
            if (mapped != null) {
                return mapped;
            }
        }

        if (profile.getEmployeeType() != null) {
            String mapped = mappings.get(profile.getEmployeeType());
            if (mapped != null) {
                return mapped;
            }
        }

        return DEFAULT_ROLE;
    }
}
