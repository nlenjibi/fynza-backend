package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.NoShowReportRecordDto;
import com.aoms.aomsbackend.attendance.repository.NoShowReadModelRepository;
import com.aoms.aomsbackend.attendance.service.impl.NoShowReportServiceImpl;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoShowReportServiceTest {

    @Mock private NoShowReadModelRepository noShowRepo;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpSession session;

    private NoShowReportServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORG_ID  = UUID.randomUUID();

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO   = LocalDate.of(2026, 3, 31);

    @BeforeEach
    void setUp() {
        service = new NoShowReportServiceImpl(noShowRepo, userRoleRepository);
        when(httpRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey()))
                .thenReturn(USER_ID.toString());
    }

    // ── role guard ─────────────────────────────────────────────────────────────

    @Test
    void getReport_withEmployeeRole_throwsForbiddenException() {
        stubRoles("ROLE_EMPLOYEE");

        assertThatThrownBy(() -> service.getReport(
                httpRequest, FROM, TO, null, null, null, 0, 20))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(noShowRepo);
    }

    @Test
    void getReport_withManagerRole_throwsForbiddenException() {
        stubRoles("ROLE_MANAGER");

        assertThatThrownBy(() -> service.getReport(
                httpRequest, FROM, TO, null, null, null, 0, 20))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(noShowRepo);
    }

    @Test
    void getReport_withHrRole_throwsForbiddenException() {
        stubRoles("ROLE_HR");

        assertThatThrownBy(() -> service.getReport(
                httpRequest, FROM, TO, null, null, null, 0, 20))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getReport_withFacilitiesAdminRole_returns200() {
        stubRoles("ROLE_FACILITIES_ADMIN");
        stubFacilitiesAdminOrgLookup();
        stubEmptyRepoPage();

        PaginatedResponse<NoShowReportRecordDto> result =
                service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getReport_withSuperAdminRole_returns200() {
        stubRoles("ROLE_SUPER_ADMIN");
        stubEmptyRepoPage();

        PaginatedResponse<NoShowReportRecordDto> result =
                service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20);

        assertThat(result).isNotNull();
    }

    // ── FACILITIES_ADMIN org scoping ───────────────────────────────────────────

    @Test
    void getReport_facilitiesAdmin_scopedToOwnOrganisation() {
        stubRoles("ROLE_FACILITIES_ADMIN");
        stubFacilitiesAdminOrgLookup();
        stubEmptyRepoPage();

        service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20);

        verify(noShowRepo).findReportPage(eq(ORG_ID), any(), any(), any(), any(), any());
    }

    @Test
    void getReport_superAdmin_withNullOrgId_queriesAllOrganisations() {
        stubRoles("ROLE_SUPER_ADMIN");
        stubEmptyRepoPage();

        service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20);

        verify(noShowRepo).findReportPage(isNull(), any(), any(), any(), any(), any());
    }

    @Test
    void getReport_superAdmin_withExplicitOrgId_passesOrgIdToRepo() {
        stubRoles("ROLE_SUPER_ADMIN");
        stubEmptyRepoPage();

        service.getReport(httpRequest, FROM, TO, null, null, ORG_ID, 0, 20);

        verify(noShowRepo).findReportPage(eq(ORG_ID), any(), any(), any(), any(), any());
    }

    // ── no_show_count_in_period ────────────────────────────────────────────────

    @Test
    void getReport_twoRecordsForSameEmployee_countReflectsPerEmployeeTotal() {
        stubRoles("ROLE_SUPER_ADMIN");

        UUID empId = UUID.randomUUID();
        NoShowReportRecordDto record1 = buildRecord(empId, FROM.plusDays(1), 2);
        NoShowReportRecordDto record2 = buildRecord(empId, FROM.plusDays(5), 2);

        when(noShowRepo.findReportPage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(record1, record2), PageRequest.of(0, 20), 2));

        PaginatedResponse<NoShowReportRecordDto> result =
                service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(2);
        result.getContent().forEach(r ->
                assertThat(r.getNoShowCountInPeriod()).isEqualTo(2));
    }

    // ── optional filters forwarded ─────────────────────────────────────────────

    @Test
    void getReport_withEmployeeIdFilter_forwardsToRepo() {
        UUID targetEmployee = UUID.randomUUID();
        stubRoles("ROLE_SUPER_ADMIN");
        stubEmptyRepoPage();

        service.getReport(httpRequest, FROM, TO, targetEmployee, null, null, 0, 20);

        verify(noShowRepo).findReportPage(any(), any(), any(), eq(targetEmployee), any(), any());
    }

    @Test
    void getReport_withDepartmentFilter_forwardsToRepo() {
        stubRoles("ROLE_SUPER_ADMIN");
        stubEmptyRepoPage();

        service.getReport(httpRequest, FROM, TO, null, "Engineering", null, 0, 20);

        verify(noShowRepo).findReportPage(any(), any(), any(), any(), eq("Engineering"), any());
    }

    // ── CSV export window cap ──────────────────────────────────────────────────

    @Test
    void exportCsv_with91DayWindow_throwsBadRequestException() {
        stubRoles("ROLE_SUPER_ADMIN");

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = from.plusDays(91);

        assertThatThrownBy(() ->
                service.exportCsv(httpRequest, from, to, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("90");
    }

    @Test
    void exportCsv_with90DayWindow_doesNotThrow() {
        stubRoles("ROLE_SUPER_ADMIN");

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = from.plusDays(90);

        stubEmptyRepoPage();

        assertThat(service.exportCsv(httpRequest, from, to, null, null, null))
                .isNotNull();
    }

    // ── session handling ───────────────────────────────────────────────────────

    @Test
    void getReport_withNoSession_throwsSessionExpiredException() {
        when(httpRequest.getSession(false)).thenReturn(null);

        assertThatThrownBy(() ->
                service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void getReport_withV2AuthSession_resolvesUserCorrectly() {
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(null);
        when(session.getAttribute(SessionAttribute.V2_USER_ID.getKey()))
                .thenReturn(USER_ID.toString());
        when(session.getAttribute(SessionAttribute.V2_ROLES.getKey()))
                .thenReturn(List.of("ROLE_SUPER_ADMIN"));
        stubEmptyRepoPage();

        assertThat(service.getReport(httpRequest, FROM, TO, null, null, null, 0, 20))
                .isNotNull();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void stubRoles(String... roles) {
        when(session.getAttribute(SessionAttribute.ROLES.getKey()))
                .thenReturn(List.of(roles));
    }

    private void stubFacilitiesAdminOrgLookup() {
        UserRole role = UserRole.builder()
                .userId(USER_ID)
                .role(UserRoleType.FACILITIES_ADMIN)
                .organisationId(ORG_ID)
                .build();
        when(userRoleRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(List.of(role));
    }

    private void stubEmptyRepoPage() {
        when(noShowRepo.findReportPage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private NoShowReportRecordDto buildRecord(UUID employeeId, LocalDate date, int count) {
        return NoShowReportRecordDto.builder()
                .noShowRecordId(UUID.randomUUID())
                .employeeId(employeeId)
                .employeeName("Jane Doe")
                .department("Engineering")
                .bookingDate(date)
                .seatReference("Floor 2 / Zone A / Seat 14")
                .autoReleasedAt(Instant.now())
                .noShowCountInPeriod(count)
                .build();
    }
}
