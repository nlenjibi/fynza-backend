package com.aoms.aomsbackend.seating.service.impl;

import com.aoms.aomsbackend.attendance.dto.CreateSeatBookingRequest;
import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import com.aoms.aomsbackend.common.exception.ResourceNotFoundException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.seating.dto.BookingResponse;
import com.aoms.aomsbackend.seating.dto.CreateBookingRequest;
import com.aoms.aomsbackend.seating.entity.*;
import com.aoms.aomsbackend.seating.event.SeatBookedEvent;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.service.SeatBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatBookingServiceImpl implements SeatBookingService {

    private final SeatRepository seatRepository;
    private final SeatBookingRepository seatBookingRepository;
    private final LocationConfigRepository locationConfigRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BookingResponse createBooking(HttpServletRequest request, CreateBookingRequest req) {
        UUID userId = resolveUserId(request);

        validateNotPast(req.getBookingDate());

        Seat seat = seatRepository.findByIdWithLock(req.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found."));

        validateHotDeskType(seat);
        validateSeatBookable(seat);
        validateBookingWindow(seat.getBuildingId(), req.getBookingDate());
        validateNoDuplicateForUser(userId, req.getBookingDate());

        SeatBooking booking = buildBooking(userId, seat, req.getBookingDate());
        booking = persistBooking(booking);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(userId)
                .actorRole(resolveActorRole(request))
                .action("SEAT_BOOKED")
                .entityType("SeatBooking")
                .entityId(booking.getId())
                .locationId(seat.getBuildingId())
                .build());
        eventPublisher.publishEvent(
                new SeatBookedEvent(booking.getId(), userId, seat.getId(), req.getBookingDate()));

        return toResponse(booking);
    }

    @Override
    public List<BookingResponse> getMyBookings(HttpServletRequest request) {
        UUID userId = resolveUserId(request);
        return seatBookingRepository.findByUserIdOrderByBookingDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BookingResponse getBookingById(HttpServletRequest request, UUID bookingId) {
        UUID userId = resolveUserId(request);
        return seatBookingRepository.findByIdAndUserId(bookingId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
    }

    // ── session ─────────────────────────────────────────────────────────────────

    private UUID resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException("Session is invalid or expired.");
        }
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());
        }
        if (userId == null) {
            throw new SessionExpiredException("Session is invalid or expired.");
        }
        return UUID.fromString(userId);
    }

    @SuppressWarnings("unchecked")
    private UserRoleType resolveActorRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return UserRoleType.EMPLOYEE;
        java.util.List<String> roles = (java.util.List<String>) session.getAttribute(SessionAttribute.ROLES.getKey());
        if (roles == null) {
            roles = (java.util.List<String>) session.getAttribute(SessionAttribute.V2_ROLES.getKey());
        }
        if (roles == null || roles.isEmpty()) return UserRoleType.EMPLOYEE;
        return roles.stream()
                .map(r -> r.replace("ROLE_", ""))
                .map(r -> { try { return UserRoleType.valueOf(r); } catch (IllegalArgumentException e) { return null; } })
                .filter(java.util.Objects::nonNull)
                .max(java.util.Comparator.comparingInt(UserRoleType::getRank))
                .orElse(UserRoleType.EMPLOYEE);
    }



    private void validateNotPast(LocalDate bookingDate) {
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past.");
        }
    }

    private void validateHotDeskType(Seat seat) {
        if (seat.getSeatType() != SeatType.HOT_DESK) {
            throw new BadRequestException("Only hot-desk seats can be booked through this endpoint.");
        }
    }

    private void validateSeatBookable(Seat seat) {
        if (!seat.isBookable()) {
            throw new ConflictException("This seat is not available for booking.", "SEAT_UNAVAILABLE");
        }
    }

    private void validateBookingWindow(UUID locationId, LocalDate bookingDate) {
        int windowDays = locationConfigRepository.findByBuildingId(locationId)
                .map(LocationConfig::getHotDeskBookingWindowDays)
                .orElse(30);
        if (bookingDate.isAfter(LocalDate.now().plusDays(windowDays))) {
            throw new BadRequestException(
                    "Booking date exceeds the allowed advance booking window of " + windowDays + " days.");
        }
    }

    private void validateNoDuplicateForUser(UUID userId, LocalDate bookingDate) {
        if (seatBookingRepository.existsByUserIdAndBookingDateAndStatus(
                userId, bookingDate, SeatBookingStatus.CONFIRMED)) {
            throw new ConflictException("You already have a confirmed booking for this date.",
                    "ALREADY_BOOKED");
        }
    }

    // ── persistence ───────────────────────────────────────────────────────────────

    private SeatBooking buildBooking(UUID userId, Seat seat, LocalDate bookingDate) {
        return SeatBooking.builder()
                .userId(userId)
                .seatId(seat.getId())
                .bookingDate(bookingDate)
                .buildingId(seat.getBuildingId())
                .status(SeatBookingStatus.CONFIRMED)
                .build();
    }

    private SeatBooking persistBooking(SeatBooking booking) {
        try {
            return seatBookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This seat is already booked for the selected date.",
                    "SEAT_UNAVAILABLE");
        }
    }

    // ── mapping ───────────────────────────────────────────────────────────────────

    private BookingResponse toResponse(SeatBooking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .seatId(booking.getSeatId())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus().name())
                .buildingId(booking.getBuildingId())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    // ── cancellation ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SeatBookingResponse cancelBooking(UUID buildingId, UUID bookingId,
                                             UUID actingUserId, UserRoleType actingRole,
                                             String cancellationReason) {
        SeatBooking booking = loadAndScope(buildingId, bookingId);

        if (booking.getStatus() == SeatBookingStatus.CANCELLED
                || booking.getStatus() == SeatBookingStatus.RELEASED) {
            throw new ConflictException(
                    "Booking is already " + booking.getStatus().name().toLowerCase().replace('_', '-') + ".",
                    "BOOKING_ALREADY_INACTIVE");
        }

        boolean isAdmin = isPrivilegedRole(actingRole);

        if (!isAdmin) {
            if (!booking.getUserId().equals(actingUserId)) {
                throw new ForbiddenException();
            }
            assertCutoffNotPassed(booking, buildingId);
        }

        booking.setStatus(SeatBookingStatus.CANCELLED);
        booking.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
        booking.setCancellationReason(cancellationReason);
        booking = seatBookingRepository.save(booking);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(actingUserId)
                .actorRole(actingRole)
                .action("SEAT_BOOKING_CANCELLED")
                .entityType("SeatBooking")
                .entityId(bookingId)
                .locationId(buildingId)
                .build());

        log.info("Booking cancelled: bookingId={}, cancelledBy={}, role={}",
                bookingId, actingUserId, actingRole);

        return toSeatBookingResponse(booking);
    }

    @Override
    public SeatBookingResponse getBooking(UUID buildingId, UUID bookingId,
                                          UUID actingUserId, UserRoleType actingRole) {
        SeatBooking booking = loadAndScope(buildingId, bookingId);

        if (!isPrivilegedRole(actingRole) && !booking.getUserId().equals(actingUserId)) {
            throw new ForbiddenException();
        }

        return toSeatBookingResponse(booking);
    }

    private SeatBooking loadAndScope(UUID buildingId, UUID bookingId) {
        return seatBookingRepository.findByIdAndBuildingId(bookingId, buildingId)
                .orElseThrow(() -> new NotFoundException(
                        "Booking not found: id=" + bookingId + ", buildingId=" + buildingId));
    }

    private boolean isPrivilegedRole(UserRoleType role) {
        return role == UserRoleType.FACILITIES_ADMIN
                || role == UserRoleType.MANAGER
                || role == UserRoleType.HR
                || role == UserRoleType.SUPER_ADMIN;
    }

    private void assertCutoffNotPassed(SeatBooking booking, UUID buildingId) {
        int cutoffHours = locationConfigRepository.findByBuildingId(buildingId)
                .map(LocationConfig::getBookingCancellationCutoffHours)
                .orElse(2);

        OffsetDateTime cutoffDeadline = booking.getBookingDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime()
                .minusHours(cutoffHours);

        if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(cutoffDeadline)) {
            throw new BadRequestException(
                    "Cancellation window has passed. Bookings must be cancelled at least "
                            + cutoffHours + " hour(s) before midnight of the booking date.",
                    "CANCELLATION_CUTOFF_PASSED");
        }
    }

    @Override
    @Transactional
    public SeatBookingResponse createSeatBooking(UUID employeeId, CreateSeatBookingRequest request) {
        Optional<SeatBooking> placeholder = seatBookingRepository
                .findBySeatIdAndBookingDateAndStatusAndBlockReservationIdIsNotNull(
                        request.getSeatId(), request.getBookingDate(), SeatBookingStatus.CONFIRMED);

        if (placeholder.isPresent()) {
            // Team member is claiming a seat within a block reservation.
            // Update the existing placeholder in-place to avoid a unique-constraint violation
            // on (seat_id, booking_date) for CONFIRMED status.
            SeatBooking booking = placeholder.get();
            booking.setUserId(employeeId);
            SeatBooking saved = seatBookingRepository.save(booking);
            log.info("Employee {} claimed block seat {} on {} (booking {})",
                    employeeId, request.getSeatId(), request.getBookingDate(), saved.getId());
            return toSeatBookingResponse(saved);
        }

        if (seatBookingRepository.existsBySeatIdAndBookingDateAndStatus(
                request.getSeatId(), request.getBookingDate(), SeatBookingStatus.CONFIRMED)) {
            throw new BadRequestException("Seat is already booked for that date");
        }

        SeatBooking booking = SeatBooking.builder()
                .seatId(request.getSeatId())
                .userId(employeeId)
                .buildingId(request.getBuildingId())
                .bookingDate(request.getBookingDate())
                .status(SeatBookingStatus.CONFIRMED)
                .build();
        SeatBooking saved = seatBookingRepository.save(booking);
        log.info("Employee {} booked seat {} on {} (booking {})",
                employeeId, request.getSeatId(), request.getBookingDate(), saved.getId());
        return toSeatBookingResponse(saved);
    }

    private SeatBookingResponse toSeatBookingResponse(SeatBooking booking) {
        return SeatBookingResponse.builder()
                .id(booking.getId())
                .seatId(booking.getSeatId())
                .userId(booking.getUserId())
                .buildingId(booking.getBuildingId())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .blockReservationId(booking.getBlockReservationId())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .autoReleasedAt(booking.getAutoReleasedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
