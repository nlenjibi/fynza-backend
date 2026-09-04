package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.request.CreateFloorRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateFloorRequest;
import com.aoms.aomsbackend.seating.dto.response.FloorResponse;
import com.aoms.aomsbackend.seating.entity.Floor;
import com.aoms.aomsbackend.seating.exception.FloorNotFoundException;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.repository.ZoneRepository;
import com.aoms.aomsbackend.seating.service.impl.FloorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloorServiceImplTest {

    @Mock private FloorRepository floorRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private SeatRepository seatRepository;

    @InjectMocks private FloorServiceImpl service;

    private UUID buildingId;
    private UUID floorId;
    private Floor floor;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        floor = Floor.builder()
                .id(floorId)
                .buildingId(buildingId)
                .name("Ground Floor")
                .floorNumber(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void listFloors_returnsActiveFloors() {
        when(floorRepository.findByBuildingIdAndActiveTrueAndDeletedAtIsNull(buildingId)).thenReturn(List.of(floor));

        List<FloorResponse> result = service.listFloors(buildingId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(floorId);
        assertThat(result.getFirst().getName()).isEqualTo("Ground Floor");
        assertThat(result.getFirst().getFloorNumber()).isZero();
        assertThat(result.getFirst().isActive()).isTrue();
    }

    @Test
    void listFloors_returnsEmptyListWhenNone() {
        when(floorRepository.findByBuildingIdAndActiveTrueAndDeletedAtIsNull(buildingId)).thenReturn(List.of());

        assertThat(service.listFloors(buildingId)).isEmpty();
    }

    @Test
    void getFloor_found_returnsResponse() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)).thenReturn(Optional.of(floor));

        FloorResponse result = service.getFloor(buildingId, floorId);

        assertThat(result.getId()).isEqualTo(floorId);
        assertThat(result.getBuildingId()).isEqualTo(buildingId);
    }

    @Test
    void getFloor_notFound_throwsFloorNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFloor(buildingId, floorId))
                .isInstanceOf(FloorNotFoundException.class)
                .hasMessageContaining(floorId.toString());
    }

    @Test
    void createFloor_savesAndMapsResponse() {
        CreateFloorRequest request = new CreateFloorRequest("Level 1", 1);
        when(floorRepository.save(any(Floor.class))).thenAnswer(inv -> {
            Floor f = inv.getArgument(0);
            return Floor.builder()
                    .id(UUID.randomUUID())
                    .buildingId(f.getBuildingId())
                    .name(f.getName())
                    .floorNumber(f.getFloorNumber())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        FloorResponse result = service.createFloor(buildingId, request);

        assertThat(result.getName()).isEqualTo("Level 1");
        assertThat(result.getFloorNumber()).isEqualTo(1);
        assertThat(result.getBuildingId()).isEqualTo(buildingId);
        verify(floorRepository).save(argThat(f -> f.getName().equals("Level 1") && f.getBuildingId().equals(buildingId)));
    }

    @Test
    void updateFloor_updatesNameAndFloorNumber() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorResponse result = service.updateFloor(buildingId, floorId, new UpdateFloorRequest("Updated", 2));

        assertThat(result.getName()).isEqualTo("Updated");
        assertThat(result.getFloorNumber()).isEqualTo(2);
    }

    @Test
    void updateFloor_notFound_throwsFloorNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFloor(buildingId, floorId, new UpdateFloorRequest("X", 1)))
                .isInstanceOf(FloorNotFoundException.class);
    }

    @Test
    void deactivateFloor_setsInactiveAndCascadesToSeatsThenZones() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateFloor(buildingId, floorId);

        verify(floorRepository).save(argThat(f -> !f.isActive()));
        verify(seatRepository).deactivateAllByFloorId(floorId);
        verify(zoneRepository).deactivateAllByFloorId(floorId);
    }

    @Test
    void deactivateFloor_notFound_throwsFloorNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateFloor(buildingId, floorId))
                .isInstanceOf(FloorNotFoundException.class);
    }
}
