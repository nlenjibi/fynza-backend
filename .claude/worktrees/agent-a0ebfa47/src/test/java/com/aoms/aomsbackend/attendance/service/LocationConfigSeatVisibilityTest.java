package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.LocationConfigResponse;
import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.attendance.entity.LocationConfigHistory;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.repository.LocationConfigHistoryRepository;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.attendance.service.impl.LocationConfigServiceImpl;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationConfigSeatVisibilityTest {

    @Mock
    private LocationConfigRepository repository;

    @Mock
    private LocationConfigHistoryRepository historyRepository;

    @InjectMocks
    private LocationConfigServiceImpl service;

    private UUID buildingId;
    private UUID actorId;
    private LocationConfig existingConfig;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        existingConfig = new LocationConfig();
        existingConfig.setBuildingId(buildingId);
        existingConfig.setWorkStartTime(LocalTime.of(9, 0));
        existingConfig.setLatenessThresholdMinutes(15);
        existingConfig.setMinPresenceDurationMinutes(360);
        existingConfig.setSeatVisibilityMode(SeatVisibilityMode.FULL);
        existingConfig.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void updateSeatVisibility_changesMode_returnsUpdatedResponse() {
        when(repository.findByBuildingId(buildingId)).thenReturn(Optional.of(existingConfig));
        when(repository.save(existingConfig)).thenReturn(existingConfig);

        LocationConfigResponse result = service.updateSeatVisibility(
                buildingId, new UpdateSeatVisibilityRequest(SeatVisibilityMode.TEAM_ONLY), actorId);

        assertThat(result.getSeatVisibilityMode()).isEqualTo(SeatVisibilityMode.TEAM_ONLY);
        assertThat(result.getBuildingId()).isEqualTo(buildingId);
    }

    @Test
    void updateSeatVisibility_savesHistoryWithCorrectFields() {
        when(repository.findByBuildingId(buildingId)).thenReturn(Optional.of(existingConfig));
        when(repository.save(existingConfig)).thenReturn(existingConfig);

        service.updateSeatVisibility(
                buildingId, new UpdateSeatVisibilityRequest(SeatVisibilityMode.AVAILABILITY_ONLY), actorId);

        ArgumentCaptor<LocationConfigHistory> captor = ArgumentCaptor.forClass(LocationConfigHistory.class);
        verify(historyRepository).save(captor.capture());
        LocationConfigHistory history = captor.getValue();

        assertThat(history.getBuildingId()).isEqualTo(buildingId);
        assertThat(history.getPreviousMode()).isEqualTo(SeatVisibilityMode.FULL);
        assertThat(history.getNewMode()).isEqualTo(SeatVisibilityMode.AVAILABILITY_ONLY);
        assertThat(history.getChangedBy()).isEqualTo(actorId);
    }

    @Test
    void updateSeatVisibility_previousModeNull_recordsNullPreviousMode() {
        existingConfig.setSeatVisibilityMode(null);
        when(repository.findByBuildingId(buildingId)).thenReturn(Optional.of(existingConfig));
        when(repository.save(existingConfig)).thenReturn(existingConfig);

        service.updateSeatVisibility(
                buildingId, new UpdateSeatVisibilityRequest(SeatVisibilityMode.FULL), actorId);

        ArgumentCaptor<LocationConfigHistory> captor = ArgumentCaptor.forClass(LocationConfigHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getPreviousMode()).isNull();
    }

    @Test
    void updateSeatVisibility_configNotFound_throwsNotFoundException() {
        when(repository.findByBuildingId(buildingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSeatVisibility(
                buildingId, new UpdateSeatVisibilityRequest(SeatVisibilityMode.FULL), actorId))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(historyRepository);
    }

    @Test
    void updateSeatVisibility_sameMode_stillSavesAndRecordsHistory() {
        when(repository.findByBuildingId(buildingId)).thenReturn(Optional.of(existingConfig));
        when(repository.save(existingConfig)).thenReturn(existingConfig);

        service.updateSeatVisibility(
                buildingId, new UpdateSeatVisibilityRequest(SeatVisibilityMode.FULL), actorId);

        verify(repository).save(existingConfig);
        verify(historyRepository).save(any(LocationConfigHistory.class));
    }
}
