package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.request.CreateZoneRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateZoneRequest;
import com.aoms.aomsbackend.seating.dto.response.ZoneResponse;
import com.aoms.aomsbackend.seating.entity.Floor;
import com.aoms.aomsbackend.seating.entity.Zone;
import com.aoms.aomsbackend.seating.exception.FloorNotFoundException;
import com.aoms.aomsbackend.seating.exception.ZoneNotFoundException;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.repository.ZoneRepository;
import com.aoms.aomsbackend.seating.service.impl.ZoneServiceImpl;
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
class ZoneServiceImplTest {

    @Mock private ZoneRepository zoneRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private SeatRepository seatRepository;

    @InjectMocks private ZoneServiceImpl service;

    private UUID buildingId;
    private UUID floorId;
    private UUID zoneId;
    private Floor floor;
    private Zone zone;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        zoneId = UUID.randomUUID();

        floor = Floor.builder()
                .id(floorId)
                .buildingId(buildingId)
                .name("Ground Floor")
                .floorNumber(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        zone = Zone.builder()
                .id(zoneId)
                .floorId(floorId)
                .buildingId(buildingId)
                .name("Zone A")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void stubFloor() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId))
                .thenReturn(Optional.of(floor));
    }

    @Test
    void listZones_returnsActiveZones() {
        stubFloor();
        when(zoneRepository.findByFloorIdAndActiveTrueAndDeletedAtIsNull(floorId)).thenReturn(List.of(zone));

        List<ZoneResponse> result = service.listZones(buildingId, floorId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Zone A");
        assertThat(result.getFirst().getFloorId()).isEqualTo(floorId);
        assertThat(result.getFirst().getBuildingId()).isEqualTo(buildingId);
    }

    @Test
    void listZones_floorNotFound_throwsFloorNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listZones(buildingId, floorId))
                .isInstanceOf(FloorNotFoundException.class);
    }

    @Test
    void getZone_found_returnsResponse() {
        stubFloor();
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)).thenReturn(Optional.of(zone));

        ZoneResponse result = service.getZone(buildingId, floorId, zoneId);

        assertThat(result.getId()).isEqualTo(zoneId);
        assertThat(result.getName()).isEqualTo("Zone A");
    }

    @Test
    void getZone_zoneNotFound_throwsZoneNotFoundException() {
        stubFloor();
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getZone(buildingId, floorId, zoneId))
                .isInstanceOf(ZoneNotFoundException.class)
                .hasMessageContaining(zoneId.toString());
    }

    @Test
    void createZone_savesAndReturnsResponse() {
        stubFloor();
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> {
            Zone z = inv.getArgument(0);
            return Zone.builder()
                    .id(UUID.randomUUID())
                    .floorId(z.getFloorId())
                    .buildingId(z.getBuildingId())
                    .name(z.getName())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        ZoneResponse result = service.createZone(buildingId, floorId, new CreateZoneRequest("Zone B"));

        assertThat(result.getName()).isEqualTo("Zone B");
        assertThat(result.getFloorId()).isEqualTo(floorId);
        assertThat(result.getBuildingId()).isEqualTo(buildingId);
        verify(zoneRepository).save(argThat(z -> z.getName().equals("Zone B") && z.getFloorId().equals(floorId)));
    }

    @Test
    void createZone_floorNotFound_throwsFloorNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createZone(buildingId, floorId, new CreateZoneRequest("Zone B")))
                .isInstanceOf(FloorNotFoundException.class);
    }

    @Test
    void updateZone_updatesName() {
        stubFloor();
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)).thenReturn(Optional.of(zone));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoneResponse result = service.updateZone(buildingId, floorId, zoneId, new UpdateZoneRequest("Zone C"));

        assertThat(result.getName()).isEqualTo("Zone C");
    }

    @Test
    void updateZone_zoneNotFound_throwsZoneNotFoundException() {
        stubFloor();
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateZone(buildingId, floorId, zoneId, new UpdateZoneRequest("Zone C")))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void deactivateZone_setsInactiveAndCascadesToSeats() {
        stubFloor();
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)).thenReturn(Optional.of(zone));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateZone(buildingId, floorId, zoneId);

        verify(zoneRepository).save(argThat(z -> !z.isActive()));
        verify(seatRepository).deactivateAllByZoneId(zoneId);
    }

    @Test
    void deactivateZone_zoneNotFound_throwsZoneNotFoundException() {
        stubFloor();
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateZone(buildingId, floorId, zoneId))
                .isInstanceOf(ZoneNotFoundException.class);
    }
}
