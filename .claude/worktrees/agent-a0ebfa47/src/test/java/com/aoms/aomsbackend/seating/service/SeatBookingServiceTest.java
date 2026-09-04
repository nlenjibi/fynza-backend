package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.ResourceNotFoundException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.seating.dto.BookingResponse;
import com.aoms.aomsbackend.seating.dto.CreateBookingRequest;
import com.aoms.aomsbackend.seating.entity.*;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.service.impl.SeatBookingServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatBookingServiceTest {

    @Mock private SeatRepository seatRepository;
    @Mock private SeatBookingRepository seatBookingRepository;
    @Mock private LocationConfigRepository locationConfigRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;

    private SeatBookingServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final UUID ORG_ID  = UUID.randomUUID();
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        service = new SeatBookingServiceImpl(
                seatRepository, seatBookingRepository, locationConfigRepository,
                auditLogService, eventPublisher);

        lenient().when(request.getSession(false)).thenReturn(session);
        lenient().when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(USER_ID.toString());
    }

    @Test
    void createBooking_happyPath_returnsBookingResponse() {
        Seat seat = buildSeat(SeatType.HOT_DESK, SeatStatus.AVAILABLE);
        LocationConfig config = new LocationConfig();
        config.setBuildingId(ORG_ID);
        config.setHotDeskBookingWindowDays(14);
        SeatBooking saved = buildSavedBooking();

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(USER_ID.toString());
        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        when(locationConfigRepository.findByBuildingId(ORG_ID)).thenReturn(Optional.of(config));
        when(seatBookingRepository.existsByUserIdAndBookingDateAndStatus(USER_ID, TOMORROW, SeatBookingStatus.CONFIRMED))
                .thenReturn(false);
        when(seatBookingRepository.save(any(SeatBooking.class))).thenReturn(saved);

        BookingResponse response = service.createBooking(request, new CreateBookingRequest(SEAT_ID, TOMORROW));

        assertThat(response.getBookingId()).isEqualTo(saved.getId());
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        verify(auditLogService).log(any(AuditLogEntry.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void createBooking_withPastDate_throwsBadRequest() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CreateBookingRequest bookingRequest = new CreateBookingRequest(SEAT_ID, yesterday);

        assertThatThrownBy(() -> service.createBooking(request, bookingRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    void createBooking_withNonHotDeskSeat_throwsBadRequest() {
        Seat seat = buildSeat(SeatType.DEDICATED, SeatStatus.AVAILABLE);
        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        CreateBookingRequest bookingRequest = new CreateBookingRequest(SEAT_ID, TOMORROW);

        assertThatThrownBy(() -> service.createBooking(request, bookingRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hot-desk");
    }

    @Test
    void createBooking_withUnavailableSeat_throwsConflict() {
        Seat seat = buildSeat(SeatType.HOT_DESK, SeatStatus.UNAVAILABLE);
        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        CreateBookingRequest bookingRequest = new CreateBookingRequest(SEAT_ID, TOMORROW);

        assertThatThrownBy(() -> service.createBooking(request, bookingRequest))
                .isInstanceOf(ConflictException.class)
                .extracting("code.md").isEqualTo("SEAT_UNAVAILABLE");
    }

    @Test
    void createBooking_whenUserAlreadyHasBookingOnDate_throwsConflict() {
        Seat seat = buildSeat(SeatType.HOT_DESK, SeatStatus.AVAILABLE);
        LocationConfig config = new LocationConfig();
        config.setBuildingId(ORG_ID);
        config.setHotDeskBookingWindowDays(14);

        when(seatRepository.findByIdWithLock(SEAT_ID)).thenReturn(Optional.of(seat));
        when(locationConfigRepository.findByBuildingId(ORG_ID)).thenReturn(Optional.of(config));
        when(seatBookingRepository.existsByUserIdAndBookingDateAndStatus(USER_ID, TOMORROW, SeatBookingStatus.CONFIRMED))
                .thenReturn(true);
        CreateBookingRequest bookingRequest = new CreateBookingRequest(SEAT_ID, TOMORROW);

        assertThatThrownBy(() -> service.createBooking(request, bookingRequest))
                .isInstanceOf(ConflictException.class)
                .extracting("code.md").isEqualTo("ALREADY_BOOKED");
    }

    @Test
    void getBookingById_whenBookingDoesNotBelongToUser_throwsResourceNotFound() {
        UUID otherBookingId = UUID.randomUUID();
        when(seatBookingRepository.findByIdAndUserId(otherBookingId, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBookingById(request, otherBookingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void createBooking_withNoSession_throwsSessionExpired() {
        when(request.getSession(false)).thenReturn(null);
        CreateBookingRequest bookingRequest = new CreateBookingRequest(SEAT_ID, TOMORROW);

        assertThatThrownBy(() -> service.createBooking(request, bookingRequest))
                .isInstanceOf(SessionExpiredException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Seat buildSeat(SeatType type, SeatStatus status) {
        return Seat.builder()
                .id(SEAT_ID)
                .seatNumber("A1")
                .seatType(type)
                .status(status)
                .buildingId(ORG_ID)
                .build();
    }

    private SeatBooking buildSavedBooking() {
        return SeatBooking.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .seatId(SEAT_ID)
                .bookingDate(TOMORROW)
                .status(SeatBookingStatus.CONFIRMED)
                .buildingId(ORG_ID)
                .build();
    }
}