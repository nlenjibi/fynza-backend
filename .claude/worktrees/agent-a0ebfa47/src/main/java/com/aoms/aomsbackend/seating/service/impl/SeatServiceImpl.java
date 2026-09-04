package com.aoms.aomsbackend.seating.service.impl;

import com.aoms.aomsbackend.seating.dto.request.CreateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatStatusRequest;
import com.aoms.aomsbackend.seating.dto.response.SeatResponse;
import com.aoms.aomsbackend.seating.entity.Seat;
import com.aoms.aomsbackend.seating.exception.DuplicateSeatNumberException;
import com.aoms.aomsbackend.seating.exception.FloorNotFoundException;
import com.aoms.aomsbackend.seating.exception.SeatNotFoundException;
import com.aoms.aomsbackend.seating.exception.ZoneNotFoundException;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.repository.ZoneRepository;
import com.aoms.aomsbackend.seating.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ZoneRepository zoneRepository;
    private final FloorRepository floorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> listSeats(UUID buildingId, UUID floorId, UUID zoneId) {
        findFloor(buildingId, floorId);
        findZone(floorId, zoneId);
        return seatRepository.findByZoneIdAndActiveTrueAndDeletedAtIsNull(zoneId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeat(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId) {
        findFloor(buildingId, floorId);
        findZone(floorId, zoneId);
        return toResponse(findSeat(zoneId, seatId));
    }

    @Override
    public SeatResponse createSeat(UUID buildingId, UUID floorId, UUID zoneId, CreateSeatRequest request) {
        findFloor(buildingId, floorId);
        findZone(floorId, zoneId);
        if (seatRepository.existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(zoneId, request.getSeatNumber())) {
            throw new DuplicateSeatNumberException(request.getSeatNumber());
        }
        Seat seat = Seat.builder()
                .zoneId(zoneId)
                .floorId(floorId)
                .buildingId(buildingId)
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType())
                .xPosition(request.getXPosition())
                .yPosition(request.getYPosition())
                .build();
        return toResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse updateSeat(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId, UpdateSeatRequest request) {
        findFloor(buildingId, floorId);
        findZone(floorId, zoneId);
        Seat seat = findSeat(zoneId, seatId);
        if (!seat.getSeatNumber().equals(request.getSeatNumber()) &&
                seatRepository.existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(zoneId, request.getSeatNumber())) {
            throw new DuplicateSeatNumberException(request.getSeatNumber());
        }
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatType(request.getSeatType());
        seat.setXPosition(request.getXPosition());
        seat.setYPosition(request.getYPosition());
        return toResponse(seatRepository.save(seat));
    }

    @Override
    public void deactivateSeat(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId) {
        findFloor(buildingId, floorId);
        findZone(floorId, zoneId);
        Seat seat = findSeat(zoneId, seatId);
        seat.setActive(false);
        seat.setDeletedAt(Instant.now());
        seatRepository.save(seat);
    }

    @Override
    public SeatResponse updateSeatStatus(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId, UpdateSeatStatusRequest request) {
        findFloor(buildingId, floorId);
        findZone(floorId, zoneId);
        Seat seat = findSeat(zoneId, seatId);
        seat.setStatus(request.getStatus());
        return toResponse(seatRepository.save(seat));
    }

    private void findFloor(UUID buildingId, UUID floorId) {
        floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)
                .orElseThrow(() -> new FloorNotFoundException(floorId));
    }

    private void findZone(UUID floorId, UUID zoneId) {
        zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
    }

    private Seat findSeat(UUID zoneId, UUID seatId) {
        return seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));
    }

    private SeatResponse toResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .zoneId(seat.getZoneId())
                .floorId(seat.getFloorId())
                .buildingId(seat.getBuildingId())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .status(seat.getStatus())
                .assignedEmployeeId(seat.getAssignedEmployeeId())
                .xPosition(seat.getXPosition())
                .yPosition(seat.getYPosition())
                .active(seat.isActive())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }
}
