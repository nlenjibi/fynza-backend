package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.seating.service.impl.SeatBookingServiceImpl;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the cancellation and retrieval methods of {@link SeatBookingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeatBookingCancellationServiceTest {

    @Mock
    private SeatBookingRepository bookingRepository;

    @Mock
    private LocationConfigRepository locationConfigRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SeatBookingServiceImpl service;

    private static final UUID BUILDING_ID  = UUID.randomUUID();
    private static final UUID BOOKING_ID   = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID  = UUID.randomUUID();
    private static final UUID OTHER_ID     = UUID.randomUUID();
    private static final UUID ADMIN_ID     = UUID.randomUUID();

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(10);

    @BeforeEach
    void setUp() {
        LocationConfig config = new LocationConfig();
        config.setBookingCancellationCutoffHours(2);
        when(locationConfigRepository.findByBuildingId(BUILDING_ID))
                .thenReturn(Optional.of(config));

        when(bookingRepository.save(any(SeatBooking.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cancelBooking_ownConfirmedBookingInFuture_returnsCANCELLEDAndPublishesEvent() {
        SeatBooking booking = confirmedBooking(EMPLOYEE_ID, FUTURE_DATE);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        SeatBookingResponse response = service.cancelBooking(
                BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE, null);

        assertThat(response.getStatus()).isEqualTo(SeatBookingStatus.CANCELLED);
        assertThat(response.getCancelledAt()).isNotNull();
        assertThat(response.getCancellationReason()).isNull();
        verify(bookingRepository).save(booking);
        verify(auditLogService).log(any());
    }

    @Test
    void cancelBooking_withinCutoffWindow_throws400WithCorrectCode() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SeatBooking booking = confirmedBooking(EMPLOYEE_ID, yesterday);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                service.cancelBooking(BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code.md", "CANCELLATION_CUTOFF_PASSED");

        verify(bookingRepository, never()).save(any());
        verify(auditLogService, never()).log(any());
    }

    @Test
    void cancelBooking_alreadyCancelled_throws409() {
        SeatBooking booking = confirmedBooking(EMPLOYEE_ID, FUTURE_DATE);
        booking.setStatus(SeatBookingStatus.CANCELLED);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                service.cancelBooking(BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE, null))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code.md", "BOOKING_ALREADY_INACTIVE");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_autoReleased_throws409() {
        SeatBooking booking = confirmedBooking(EMPLOYEE_ID, FUTURE_DATE);
        booking.setStatus(SeatBookingStatus.RELEASED);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                service.cancelBooking(BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE, null))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code.md", "BOOKING_ALREADY_INACTIVE");
    }

    @Test
    void cancelBooking_differentUsersBooking_throws403() {
        SeatBooking booking = confirmedBooking(OTHER_ID, FUTURE_DATE);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                service.cancelBooking(BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE, null))
                .isInstanceOf(ForbiddenException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_facilitiesAdminBypassesCutoffAndOwnership_succeeds() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SeatBooking booking = confirmedBooking(EMPLOYEE_ID, yesterday);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        SeatBookingResponse response = service.cancelBooking(
                BUILDING_ID, BOOKING_ID, ADMIN_ID, UserRoleType.FACILITIES_ADMIN, "Desk urgently needed");

        assertThat(response.getStatus()).isEqualTo(SeatBookingStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("Desk urgently needed");
        assertThat(response.getCancelledAt()).isNotNull();
        verify(auditLogService).log(any());
    }

    @Test
    void getBooking_employeeSeeOwnBooking_returnsBooking() {
        SeatBooking booking = confirmedBooking(EMPLOYEE_ID, FUTURE_DATE);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        SeatBookingResponse response = service.getBooking(
                BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE);

        assertThat(response.getUserId()).isEqualTo(EMPLOYEE_ID);
        assertThat(response.getStatus()).isEqualTo(SeatBookingStatus.CONFIRMED);
    }

    @Test
    void getBooking_employeeSeesAnotherUsersBooking_throws403() {
        SeatBooking booking = confirmedBooking(OTHER_ID, FUTURE_DATE);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                service.getBooking(BUILDING_ID, BOOKING_ID, EMPLOYEE_ID, UserRoleType.EMPLOYEE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getBooking_facilitiesAdminSeesAnyBooking_returnsBooking() {
        SeatBooking booking = confirmedBooking(OTHER_ID, FUTURE_DATE);
        when(bookingRepository.findByIdAndBuildingId(BOOKING_ID, BUILDING_ID))
                .thenReturn(Optional.of(booking));

        SeatBookingResponse response = service.getBooking(
                BUILDING_ID, BOOKING_ID, ADMIN_ID, UserRoleType.FACILITIES_ADMIN);

        assertThat(response.getUserId()).isEqualTo(OTHER_ID);
    }

    private SeatBooking confirmedBooking(UUID userId, LocalDate date) {
        return SeatBooking.builder()
                .id(BOOKING_ID)
                .seatId(UUID.randomUUID())
                .userId(userId)
                .buildingId(BUILDING_ID)
                .bookingDate(date)
                .status(SeatBookingStatus.CONFIRMED)
                .build();
    }
}
