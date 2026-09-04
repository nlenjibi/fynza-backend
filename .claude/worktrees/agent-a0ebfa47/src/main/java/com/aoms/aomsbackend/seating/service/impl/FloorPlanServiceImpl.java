package com.aoms.aomsbackend.seating.service.impl;

import com.aoms.aomsbackend.attendance.entity.Employee;
import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.repository.EmployeeRepository;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import com.aoms.aomsbackend.seating.dto.FloorPlanFloorResponse;
import com.aoms.aomsbackend.seating.dto.FloorPlanResponse;
import com.aoms.aomsbackend.seating.dto.FloorPlanSeatResponse;
import com.aoms.aomsbackend.seating.dto.OccupantInfo;
import com.aoms.aomsbackend.seating.entity.Floor;
import com.aoms.aomsbackend.seating.entity.Seat;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.service.FloorPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FloorPlanServiceImpl implements FloorPlanService {

    private final SeatBookingRepository seatBookingRepository;
    private final LocationConfigRepository locationConfigRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final FloorRepository floorRepository;
    private final SeatRepository seatRepository;

    @Override
    public FloorPlanResponse getFloorPlan(UUID buildingId, UUID requestingUserId) {
        SeatVisibilityMode mode = locationConfigRepository.findByBuildingId(buildingId)
                .map(LocationConfig::getSeatVisibilityMode)
                .orElseThrow(() -> new NotFoundException("Location config not found for buildingId: " + buildingId));

        String requesterDepartment = null;
        if (mode == SeatVisibilityMode.TEAM_ONLY && requestingUserId != null) {
            requesterDepartment = resolveEmployeeDepartment(requestingUserId);
        }

        List<Floor> floors = floorRepository.findByBuildingIdAndActiveTrueAndDeletedAtIsNullOrderByFloorNumber(buildingId);
        Map<UUID, UUID> bookedSeatOccupants = seatBookingRepository
                .findByBuildingIdAndBookingDateAndStatus(buildingId, LocalDate.now(), SeatBookingStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(SeatBooking::getSeatId, SeatBooking::getUserId));

        final String department = requesterDepartment;
        List<FloorPlanFloorResponse> floorResponses = floors.stream()
                .map(floor -> buildFloorResponse(floor, mode, department, bookedSeatOccupants))
                .toList();

        return FloorPlanResponse.builder()
                .buildingId(buildingId)
                .seatVisibilityMode(mode)
                .floors(floorResponses)
                .build();
    }

    private FloorPlanFloorResponse buildFloorResponse(Floor floor, SeatVisibilityMode mode,
                                                      String requesterDepartment, Map<UUID, UUID> bookedSeatOccupants) {
        List<Seat> seats = seatRepository.findByFloorIdAndDeletedAtIsNull(floor.getId());
        Set<UUID> occupantIds = seats.stream()
                .map(seat -> determineOccupantId(seat, bookedSeatOccupants))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Employee> employeesById = loadEmployeesById(occupantIds);
        List<FloorPlanSeatResponse> seatResponses = seats.stream()
                .map(seat -> buildSeatResponse(seat, mode, requesterDepartment, bookedSeatOccupants, employeesById))
                .toList();
        return FloorPlanFloorResponse.builder()
                .floorId(floor.getId())
                .floorName(floor.getName())
                .floorNumber(floor.getFloorNumber())
                .seats(seatResponses)
                .build();
    }

    private FloorPlanSeatResponse buildSeatResponse(Seat seat, SeatVisibilityMode mode,
            String requesterDepartment, Map<UUID, UUID> bookedSeatOccupants,
            Map<UUID, Employee> employeesById) {
        UUID occupantId = determineOccupantId(seat, bookedSeatOccupants);
        boolean isOccupied = occupantId != null;
        OccupantInfo occupantInfo = isOccupied ? resolveOccupantInfo(occupantId, mode, requesterDepartment, employeesById) : null;

        return FloorPlanSeatResponse.builder()
                .seatId(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType() != null ? seat.getSeatType().name() : null)
                .status(seat.getStatus() != null ? seat.getStatus().name() : null)
                .xPosition(seat.getXPosition())
                .yPosition(seat.getYPosition())
                .occupied(isOccupied)
                .occupantInfo(occupantInfo)
                .build();
    }

    private UUID determineOccupantId(Seat seat, Map<UUID, UUID> bookedSeatOccupants) {
        if (seat.getAssignedEmployeeId() != null) {
            return seat.getAssignedEmployeeId();
        }
        return bookedSeatOccupants.get(seat.getId());
    }

    private OccupantInfo resolveOccupantInfo(UUID occupantId, SeatVisibilityMode mode, String requesterDepartment,
            Map<UUID, Employee> employeesById) {
        return switch (mode) {
            case AVAILABILITY_ONLY -> null;
            case FULL -> {
                Employee emp = employeesById.get(occupantId);
                yield emp == null ? null : toOccupantInfo(emp);
            }
            case TEAM_ONLY -> {
                if (requesterDepartment == null) yield null;
                Employee emp = employeesById.get(occupantId);
                if (emp == null || !requesterDepartment.equals(emp.getDepartment())) yield null;
                yield toOccupantInfo(emp);
            }
        };
    }

    private OccupantInfo toOccupantInfo(Employee emp) {
        return OccupantInfo.builder()
                .employeeId(emp.getId())
                .name(emp.getDisplayName())
                .department(emp.getDepartment())
                .build();
    }

    private String resolveEmployeeDepartment(UUID userId) {
        return userRepository.findById(userId)
                .flatMap(user -> employeeRepository.findBySsoUserId(user.getSsoUserId()))
                .map(Employee::getDepartment)
                .orElse(null);
    }

    private Map<UUID, Employee> loadEmployeesById(Set<UUID> occupantIds) {
        if (occupantIds.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findAllById(occupantIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }
}
