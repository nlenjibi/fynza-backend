package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.BadRequestWithCodeException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.seating.dto.AssignPermanentSeatRequest;
import com.aoms.aomsbackend.seating.dto.SeatAssignmentResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeUpdateRequest;
import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.seating.entity.Seat;
import com.aoms.aomsbackend.seating.entity.SeatStatus;
import com.aoms.aomsbackend.seating.entity.SeatType;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.service.impl.SeatManagementServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatManagementServiceTest {

    @Mock private SeatRepository seatRepository;
    @Mock private SeatBookingRepository seatBookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;

    private SeatManagementServiceImpl service;

    private static final UUID ACTOR_ID    = UUID.randomUUID();
    private static final UUID SEAT_ID     = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SeatManagementServiceImpl(
                seatRepository, seatBookingRepository, userRepository,
                userRoleRepository, auditLogService);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(ACTOR_ID.toString());
    }

    // ── assign ───────────────────────────────────────────────────────────────────

    @Test
    void assign_withNonPermanentSeat_throwsBadRequest() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.HOT_DESK, null)));

        assertThatThrownBy(() ->
                service.assign(SEAT_ID, new AssignPermanentSeatRequest(EMPLOYEE_ID), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PERMANENT");
    }

    @Test
    void assign_withAlreadyAssignedSeat_throwsConflict() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.PERMANENT, UUID.randomUUID())));

        assertThatThrownBy(() ->
                service.assign(SEAT_ID, new AssignPermanentSeatRequest(EMPLOYEE_ID), request))
                .isInstanceOf(ConflictException.class)
                .extracting("code.md").isEqualTo("SEAT_ALREADY_ASSIGNED");
    }

    @Test
    void assign_withInactiveEmployee_throwsBadRequest() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.PERMANENT, null)));
        User inactive = buildUser(false);
        when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() ->
                service.assign(SEAT_ID, new AssignPermanentSeatRequest(EMPLOYEE_ID), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void assign_withEmployeeAtDifferentLocation_throwsBadRequest() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.PERMANENT, null)));
        when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(buildUser(true)));
        when(userRoleRepository.findByUserIdAndDeletedAtIsNull(EMPLOYEE_ID))
                .thenReturn(List.of(roleAt(UUID.randomUUID())));

        assertThatThrownBy(() ->
                service.assign(SEAT_ID, new AssignPermanentSeatRequest(EMPLOYEE_ID), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("location");
    }

    @Test
    void assign_happyPath_savesAndAudits() {
        Seat seat = buildSeat(SeatType.PERMANENT, null);
        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any())).thenReturn(seat);
        when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(buildUser(true)));
        when(userRoleRepository.findByUserIdAndDeletedAtIsNull(EMPLOYEE_ID))
                .thenReturn(List.of(roleAt(LOCATION_ID)));

        SeatAssignmentResponse response =
                service.assign(SEAT_ID, new AssignPermanentSeatRequest(EMPLOYEE_ID), request);

        assertThat(response.getSeatId()).isEqualTo(SEAT_ID);
        assertThat(response.getAssignedUserId()).isEqualTo(EMPLOYEE_ID);
        verify(seatRepository).save(seat);
        verify(auditLogService).log(any(AuditLogEntry.class));
    }

    // ── unassign ─────────────────────────────────────────────────────────────────

    @Test
    void unassign_withNoCurrentAssignment_throwsBadRequest() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.PERMANENT, null)));

        assertThatThrownBy(() -> service.unassign(SEAT_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no current permanent assignment");
    }

    @Test
    void unassign_happyPath_clearsAssignmentAndAudits() {
        Seat seat = buildSeat(SeatType.PERMANENT, EMPLOYEE_ID);
        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any())).thenReturn(seat);

        SeatAssignmentResponse response = service.unassign(SEAT_ID, request);

        assertThat(response.getAssignedUserId()).isNull();
        verify(auditLogService).log(any(AuditLogEntry.class));
    }

    // ── convertType ───────────────────────────────────────────────────────────────

    @Test
    void convertType_permanentToHotDesk_withActiveAssignment_throwsBadRequestWithCode() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.PERMANENT, EMPLOYEE_ID)));

        assertThatThrownBy(() ->
                service.convertType(SEAT_ID, new SeatTypeUpdateRequest(SeatType.HOT_DESK), request))
                .isInstanceOf(BadRequestWithCodeException.class)
                .extracting("code.md").isEqualTo("SEAT_STILL_ASSIGNED");
    }

    @Test
    void convertType_hotDeskToPermanent_withFutureBookings_throwsBadRequestWithCode() {
        when(seatRepository.findByIdWithLock(SEAT_ID))
                .thenReturn(Optional.of(buildSeat(SeatType.HOT_DESK, null)));
        when(seatBookingRepository.existsBySeatIdAndBookingDateGreaterThanEqualAndStatus(
                SEAT_ID, LocalDate.now(), SeatBookingStatus.CONFIRMED)).thenReturn(true);

        assertThatThrownBy(() ->
                service.convertType(SEAT_ID, new SeatTypeUpdateRequest(SeatType.PERMANENT), request))
                .isInstanceOf(BadRequestWithCodeException.class)
                .extracting("code.md").isEqualTo("SEAT_HAS_FUTURE_BOOKINGS");
    }

    @Test
    void convertType_permanentToHotDesk_withNoAssignment_succeeds() {
        Seat seat = buildSeat(SeatType.PERMANENT, null);
        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any())).thenReturn(seat);

        SeatTypeResponse response =
                service.convertType(SEAT_ID, new SeatTypeUpdateRequest(SeatType.HOT_DESK), request);

        assertThat(response.getSeatType()).isEqualTo(SeatType.HOT_DESK);
        verify(auditLogService).log(any(AuditLogEntry.class));
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private Seat buildSeat(SeatType type, UUID permanentUserId) {
        return Seat.builder()
                .id(SEAT_ID)
                .buildingId(LOCATION_ID)
                .seatLabel("A-01")
                .seatType(type)
                .status(SeatStatus.AVAILABLE)
                .permanentUserId(permanentUserId)
                .build();
    }

    private User buildUser(boolean active) {
        return User.builder()
                .id(EMPLOYEE_ID)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .isActive(active)
                .build();
    }

    private UserRole roleAt(UUID locationId) {
        return UserRole.builder()
                .userId(EMPLOYEE_ID)
                .role(UserRoleType.EMPLOYEE)
                .organisationId(locationId)
                .build();
    }
}
