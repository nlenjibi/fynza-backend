package com.aoms.aomsbackend.audit.service;

import com.aoms.aomsbackend.audit.dto.AuditLogFilter;
import com.aoms.aomsbackend.audit.dto.AuditLogResponse;
import com.aoms.aomsbackend.audit.repository.AuditLogRepository;
import com.aoms.aomsbackend.audit.service.impl.AuditLogQueryServiceImpl;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleLookupService;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRoleLookupService userRoleLookupService;

    @InjectMocks private AuditLogQueryServiceImpl service;

    private static final UUID ACTOR_ID   = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();

    @Test
    void query_asManager_throwsForbidden() {
        when(userRoleLookupService.getRolesForUser(ACTOR_ID))
                .thenReturn(List.of(roleOf(UserRoleType.MANAGER, LOCATION_ID)));

        assertThatThrownBy(() ->
                service.query(new AuditLogFilter(), PageRequest.of(0, 10), ACTOR_ID, LOCATION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void query_asEmployee_throwsForbidden() {
        when(userRoleLookupService.getRolesForUser(ACTOR_ID))
                .thenReturn(List.of(roleOf(UserRoleType.EMPLOYEE, LOCATION_ID)));

        assertThatThrownBy(() ->
                service.query(new AuditLogFilter(), PageRequest.of(0, 10), ACTOR_ID, LOCATION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void query_asFacilitiesAdmin_returnsPagedResults() {
        when(userRoleLookupService.getRolesForUser(ACTOR_ID))
                .thenReturn(List.of(roleOf(UserRoleType.FACILITIES_ADMIN, LOCATION_ID)));
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = service.query(
                new AuditLogFilter(), PageRequest.of(0, 10), ACTOR_ID, LOCATION_ID);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void query_asHR_returnsPagedResults() {
        when(userRoleLookupService.getRolesForUser(ACTOR_ID))
                .thenReturn(List.of(roleOf(UserRoleType.HR, LOCATION_ID)));
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = service.query(
                new AuditLogFilter(), PageRequest.of(0, 10), ACTOR_ID, LOCATION_ID);

        assertThat(result).isNotNull();
    }

    @Test
    void query_asSuperAdmin_nullLocationReturnsAllRecords() {
        when(userRoleLookupService.getRolesForUser(ACTOR_ID))
                .thenReturn(List.of(roleOf(UserRoleType.SUPER_ADMIN, null)));
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = service.query(
                new AuditLogFilter(), PageRequest.of(0, 10), ACTOR_ID, null);

        assertThat(result).isNotNull();
    }

    @Test
    void query_withActionFilter_passesSpecificationToRepository() {
        AuditLogFilter filter = new AuditLogFilter();
        filter.setAction("SEAT_BOOKED");

        when(userRoleLookupService.getRolesForUser(ACTOR_ID))
                .thenReturn(List.of(roleOf(UserRoleType.HR, LOCATION_ID)));
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = service.query(
                filter, PageRequest.of(0, 10), ACTOR_ID, LOCATION_ID);

        assertThat(result).isNotNull();
    }

    private UserRole roleOf(UserRoleType type, UUID locationId) {
        return UserRole.builder()
                .userId(ACTOR_ID)
                .role(type)
                .organisationId(locationId)
                .build();
    }
}
