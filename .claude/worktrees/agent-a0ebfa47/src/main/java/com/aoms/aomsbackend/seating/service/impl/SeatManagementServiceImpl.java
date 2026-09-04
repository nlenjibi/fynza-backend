package com.aoms.aomsbackend.seating.service.impl;

import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.BadRequestWithCodeException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.ResourceNotFoundException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.seating.dto.AssignPermanentSeatRequest;
import com.aoms.aomsbackend.seating.dto.SeatAssignmentResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeUpdateRequest;
import com.aoms.aomsbackend.seating.entity.Seat;
import com.aoms.aomsbackend.seating.entity.SeatType;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.service.SeatManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatManagementServiceImpl implements SeatManagementService {

    private final SeatRepository seatRepository;
    private final SeatBookingRepository seatBookingRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public SeatAssignmentResponse assign(UUID seatId, AssignPermanentSeatRequest req, HttpServletRequest request) {
        UUID actorId = resolveActorId(request);

        Seat seat = loadSeatWithLock(seatId);

        validateIsPermanentType(seat);
        validateNotAlreadyAssigned(seat);
        User employee = validateActiveEmployeeAtLocation(req.getUserId(), seat.getBuildingId());

        seat.setPermanentUserId(req.getUserId());
        seatRepository.save(seat);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(actorId)
                .actorRole(resolveActorRole(request))
                .action("PERMANENT_SEAT_ASSIGNED")
                .entityType("Seat")
                .entityId(seatId)
                .locationId(seat.getBuildingId())
                .newState(java.util.Map.of("assignedTo", req.getUserId().toString()))
                .build());

        return toAssignmentResponse(seat, employee);
    }

    @Override
    @Transactional
    public SeatAssignmentResponse unassign(UUID seatId, HttpServletRequest request) {
        UUID actorId = resolveActorId(request);

        Seat seat = loadSeatWithLock(seatId);

        validateHasCurrentAssignment(seat);

        seat.setPermanentUserId(null);
        seatRepository.save(seat);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(actorId)
                .actorRole(resolveActorRole(request))
                .action("PERMANENT_SEAT_UNASSIGNED")
                .entityType("Seat")
                .entityId(seatId)
                .locationId(seat.getBuildingId())
                .build());

        return toAssignmentResponse(seat, null);
    }

    @Override
    @Transactional
    public SeatTypeResponse convertType(UUID seatId, SeatTypeUpdateRequest req, HttpServletRequest request) {
        UUID actorId = resolveActorId(request);

        Seat seat = loadSeatWithLock(seatId);

        validateTypeConversion(seat, req.getSeatType());

        seat.setSeatType(req.getSeatType());
        seatRepository.save(seat);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(actorId)
                .actorRole(resolveActorRole(request))
                .action("SEAT_TYPE_CONVERTED")
                .entityType("Seat")
                .entityId(seatId)
                .locationId(seat.getBuildingId())
                .newState(java.util.Map.of("seatType", req.getSeatType().name()))
                .build());

        return SeatTypeResponse.builder()
                .seatId(seatId)
                .seatType(req.getSeatType())
                .build();
    }

    // ── session ─────────────────────────────────────────────────────────────────

    private UUID resolveActorId(HttpServletRequest request) {
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

    // ── data loading ─────────────────────────────────────────────────────────────

    private Seat loadSeatWithLock(UUID seatId) {
        return seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found."));
    }

    // ── validation ───────────────────────────────────────────────────────────────

    private void validateIsPermanentType(Seat seat) {
        if (seat.getSeatType() != SeatType.PERMANENT) {
            throw new BadRequestException(
                    "Only PERMANENT seats can have a permanent assignment.");
        }
    }

    private void validateNotAlreadyAssigned(Seat seat) {
        if (seat.getPermanentUserId() != null) {
            throw new ConflictException("This seat is already assigned to an employee.",
                    "SEAT_ALREADY_ASSIGNED");
        }
    }

    private User validateActiveEmployeeAtLocation(UUID userId, UUID locationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Employee not found."));

        if (!user.isActive()) {
            throw new BadRequestException("The specified employee is not active.");
        }

        boolean hasRoleAtLocation = userRoleRepository
                .findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .anyMatch(r -> locationId.equals(r.getOrganisationId()));

        if (!hasRoleAtLocation) {
            throw new BadRequestException(
                    "The employee does not have a role at this seat's location.");
        }

        return user;
    }

    private void validateHasCurrentAssignment(Seat seat) {
        if (seat.getPermanentUserId() == null) {
            throw new BadRequestException("This seat has no current permanent assignment.");
        }
    }

    private void validateTypeConversion(Seat seat, SeatType newType) {
        if (newType == SeatType.HOT_DESK && seat.getPermanentUserId() != null) {
            throw new BadRequestWithCodeException("SEAT_STILL_ASSIGNED",
                    "Unassign the seat before converting to HOT_DESK.");
        }
        if (newType == SeatType.PERMANENT) {
            boolean hasFutureBookings = seatBookingRepository
                    .existsBySeatIdAndBookingDateGreaterThanEqualAndStatus(
                            seat.getId(), LocalDate.now(), SeatBookingStatus.CONFIRMED);
            if (hasFutureBookings) {
                throw new BadRequestWithCodeException("SEAT_HAS_FUTURE_BOOKINGS",
                        "Cancel future bookings before converting to PERMANENT.");
            }
        }
    }

    // ── mapping ───────────────────────────────────────────────────────────────────

    private SeatAssignmentResponse toAssignmentResponse(Seat seat, User employee) {
        return SeatAssignmentResponse.builder()
                .seatId(seat.getId())
                .seatLabel(seat.getSeatLabel())
                .assignedUserId(seat.getPermanentUserId())
                .assignedUserName(employee != null ? employee.getDisplayName() : null)
                .build();
    }
}
