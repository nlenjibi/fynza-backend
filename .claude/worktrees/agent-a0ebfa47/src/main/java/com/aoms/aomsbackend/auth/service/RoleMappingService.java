package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.ArmProfile;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public interface RoleMappingService {

    Set<GrantedAuthority> mapToAuthorities(ArmProfile profile);
}
