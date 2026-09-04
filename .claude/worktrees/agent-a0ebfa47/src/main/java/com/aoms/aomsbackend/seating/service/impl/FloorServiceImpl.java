package com.aoms.aomsbackend.seating.service.impl;

import com.aoms.aomsbackend.seating.dto.request.CreateFloorRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateFloorRequest;
import com.aoms.aomsbackend.seating.dto.response.FloorResponse;
import com.aoms.aomsbackend.seating.entity.Floor;
import com.aoms.aomsbackend.seating.exception.FloorNotFoundException;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.repository.ZoneRepository;
import com.aoms.aomsbackend.seating.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FloorResponse> listFloors(UUID buildingId) {
        return floorRepository.findByBuildingIdAndActiveTrueAndDeletedAtIsNull(buildingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FloorResponse getFloor(UUID buildingId, UUID floorId) {
        return toResponse(findFloor(buildingId, floorId));
    }

    @Override
    public FloorResponse createFloor(UUID buildingId, CreateFloorRequest request) {
        Floor floor = Floor.builder()
                .buildingId(buildingId)
                .name(request.getName())
                .floorNumber(request.getFloorNumber())
                .build();
        return toResponse(floorRepository.save(floor));
    }

    @Override
    public FloorResponse updateFloor(UUID buildingId, UUID floorId, UpdateFloorRequest request) {
        Floor floor = findFloor(buildingId, floorId);
        floor.setName(request.getName());
        floor.setFloorNumber(request.getFloorNumber());
        return toResponse(floorRepository.save(floor));
    }

    @Override
    public void deactivateFloor(UUID buildingId, UUID floorId) {
        Floor floor = findFloor(buildingId, floorId);
        floor.setActive(false);
        floor.setDeletedAt(Instant.now());
        floorRepository.save(floor);
        seatRepository.deactivateAllByFloorId(floorId);
        zoneRepository.deactivateAllByFloorId(floorId);
    }

    private Floor findFloor(UUID buildingId, UUID floorId) {
        return floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)
                .orElseThrow(() -> new FloorNotFoundException(floorId));
    }

    private FloorResponse toResponse(Floor floor) {
        return FloorResponse.builder()
                .id(floor.getId())
                .buildingId(floor.getBuildingId())
                .name(floor.getName())
                .floorNumber(floor.getFloorNumber())
                .active(floor.isActive())
                .createdAt(floor.getCreatedAt())
                .updatedAt(floor.getUpdatedAt())
                .build();
    }
}
