package com.aoms.aomsbackend.seating.service.impl;

import com.aoms.aomsbackend.seating.dto.request.CreateZoneRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateZoneRequest;
import com.aoms.aomsbackend.seating.dto.response.ZoneResponse;
import com.aoms.aomsbackend.seating.entity.Zone;
import com.aoms.aomsbackend.seating.exception.FloorNotFoundException;
import com.aoms.aomsbackend.seating.exception.ZoneNotFoundException;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.repository.ZoneRepository;
import com.aoms.aomsbackend.seating.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final FloorRepository floorRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ZoneResponse> listZones(UUID buildingId, UUID floorId) {
        findFloor(buildingId, floorId);
        return zoneRepository.findByFloorIdAndActiveTrueAndDeletedAtIsNull(floorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneResponse getZone(UUID buildingId, UUID floorId, UUID zoneId) {
        findFloor(buildingId, floorId);
        return toResponse(findZone(floorId, zoneId));
    }

    @Override
    public ZoneResponse createZone(UUID buildingId, UUID floorId, CreateZoneRequest request) {
        findFloor(buildingId, floorId);
        Zone zone = Zone.builder()
                .floorId(floorId)
                .buildingId(buildingId)
                .name(request.getName())
                .build();
        return toResponse(zoneRepository.save(zone));
    }

    @Override
    public ZoneResponse updateZone(UUID buildingId, UUID floorId, UUID zoneId, UpdateZoneRequest request) {
        findFloor(buildingId, floorId);
        Zone zone = findZone(floorId, zoneId);
        zone.setName(request.getName());
        return toResponse(zoneRepository.save(zone));
    }

    @Override
    public void deactivateZone(UUID buildingId, UUID floorId, UUID zoneId) {
        findFloor(buildingId, floorId);
        Zone zone = findZone(floorId, zoneId);
        zone.setActive(false);
        zone.setDeletedAt(Instant.now());
        zoneRepository.save(zone);
        seatRepository.deactivateAllByZoneId(zoneId);
    }

    private void findFloor(UUID buildingId, UUID floorId) {
        floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)
                .orElseThrow(() -> new FloorNotFoundException(floorId));
    }

    private Zone findZone(UUID floorId, UUID zoneId) {
        return zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
    }

    private ZoneResponse toResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .floorId(zone.getFloorId())
                .buildingId(zone.getBuildingId())
                .name(zone.getName())
                .active(zone.isActive())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}
