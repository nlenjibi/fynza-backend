package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.ArmProfile;
import com.aoms.aomsbackend.auth.service.impl.RoleMappingServiceImpl;
import com.aoms.aomsbackend.config.ArmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMappingServiceTest {

    private RoleMappingServiceImpl roleMappingService;

    @BeforeEach
    void setUp() {
        ArmProperties props = new ArmProperties();
        props.setRoleMappings(Map.of(
            "Manager", "ROLE_MANAGER",
            "HR", "ROLE_HR",
            "intern", "ROLE_INTERN",
            "full_time", "ROLE_EMPLOYEE"
        ));
        roleMappingService = new RoleMappingServiceImpl(props);
    }

    @ParameterizedTest
    @CsvSource({
        "Manager, '', ROLE_MANAGER",
        "HR, '', ROLE_HR",
        "'', intern, ROLE_INTERN"
    })
    void mapToAuthorities_returnsExpectedRole(String department, String employeeType, String expectedRole) {
        ArmProfile profile = ArmProfile.builder()
            .department(department)
            .employeeType(employeeType)
            .build();

        Set<GrantedAuthority> result = roleMappingService.mapToAuthorities(profile);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
            .containsExactly(expectedRole);
    }

    @Test
    void mapToAuthorities_unknownDepartment_returnsDefaultRole() {
        ArmProfile profile = ArmProfile.builder().department("Unknown").build();

        Set<GrantedAuthority> result = roleMappingService.mapToAuthorities(profile);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }

    @Test
    void mapToAuthorities_emptyProfile_returnsDefaultRole() {
        ArmProfile profile = ArmProfile.empty();

        Set<GrantedAuthority> result = roleMappingService.mapToAuthorities(profile);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }
}
