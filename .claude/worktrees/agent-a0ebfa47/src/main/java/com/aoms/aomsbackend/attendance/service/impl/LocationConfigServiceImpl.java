package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.LocationConfigResponse;
import com.aoms.aomsbackend.attendance.dto.LocationConfigUpdateRequest;
import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.attendance.entity.LocationConfigHistory;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.repository.LocationConfigHistoryRepository;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.attendance.service.LocationConfigService;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationConfigServiceImpl implements LocationConfigService {

    private static final String NOT_FOUND_MESSAGE_PREFIX = "Location config not found for buildingId: ";

    private final LocationConfigRepository repository;
    private final LocationConfigHistoryRepository historyRepository;

    @Override
    public LocationConfigResponse getByBuildingId(UUID buildingId) {
        return repository.findByBuildingId(buildingId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE_PREFIX + buildingId));
    }

    @Override
    @Transactional
    public LocationConfigResponse updateByBuildingId(UUID buildingId, LocationConfigUpdateRequest request) {
        LocationConfig config = repository.findByBuildingId(buildingId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE_PREFIX + buildingId));

        if (request.getWorkStartTime() != null) config.setWorkStartTime(request.getWorkStartTime());
        if (request.getLatenessThresholdMinutes() != null) config.setLatenessThresholdMinutes(request.getLatenessThresholdMinutes());
        if (request.getMinPresenceDurationMinutes() != null) config.setMinPresenceDurationMinutes(request.getMinPresenceDurationMinutes());
        if (request.getNoShowReleaseTime() != null) config.setNoShowReleaseTime(request.getNoShowReleaseTime());
        if (request.getHotDeskBookingWindowDays() != null) config.setHotDeskBookingWindowDays(request.getHotDeskBookingWindowDays());
        if (request.getBookingCancellationCutoffHours() != null) config.setBookingCancellationCutoffHours(request.getBookingCancellationCutoffHours());
        if (request.getSessionGapThresholdHours() != null) config.setSessionGapThresholdHours(request.getSessionGapThresholdHours());
        config.setUpdatedAt(OffsetDateTime.now());

        return toResponse(repository.save(config));
    }

    @Override
    @Transactional
    public LocationConfigResponse updateSeatVisibility(UUID buildingId, UpdateSeatVisibilityRequest request, UUID actorId) {
        LocationConfig config = repository.findByBuildingId(buildingId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE_PREFIX + buildingId));

        SeatVisibilityMode previousMode = config.getSeatVisibilityMode();
        config.setSeatVisibilityMode(request.getSeatVisibilityMode());
        config.setUpdatedAt(OffsetDateTime.now());

        LocationConfig saved = repository.save(config);

        historyRepository.save(LocationConfigHistory.builder()
                .buildingId(buildingId)
                .previousMode(previousMode)
                .newMode(request.getSeatVisibilityMode())
                .changedBy(actorId)
                .build());

        return toResponse(saved);
    }

    private LocationConfigResponse toResponse(LocationConfig config) {
        return LocationConfigResponse.builder()
                .id(config.getId())
                .buildingId(config.getBuildingId())
                .workStartTime(config.getWorkStartTime())
                .latenessThresholdMinutes(config.getLatenessThresholdMinutes())
                .minPresenceDurationMinutes(config.getMinPresenceDurationMinutes())
                .noShowReleaseTime(config.getNoShowReleaseTime())
                .hotDeskBookingWindowDays(config.getHotDeskBookingWindowDays())
                .bookingCancellationCutoffHours(config.getBookingCancellationCutoffHours())
                .seatVisibilityMode(config.getSeatVisibilityMode())
                .sessionGapThresholdHours(config.getSessionGapThresholdHours())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
