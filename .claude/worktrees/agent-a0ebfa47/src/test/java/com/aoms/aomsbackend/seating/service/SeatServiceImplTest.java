package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.request.CreateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatStatusRequest;
import com.aoms.aomsbackend.seating.dto.response.SeatResponse;
import com.aoms.aomsbackend.seating.entity.*;
import com.aoms.aomsbackend.seating.exception.DuplicateSeatNumberException;
import com.aoms.aomsbackend.seating.exception.FloorNotFoundException;
import com.aoms.aomsbackend.seating.exception.SeatNotFoundException;
import com.aoms.aomsbackend.seating.exception.ZoneNotFoundException;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.repository.ZoneRepository;
import com.aoms.aomsbackend.seating.service.impl.SeatServiceImpl;
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
class SeatServiceImplTest {

    @Mock private SeatRepository seatRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private FloorRepository floorRepository;

    @InjectMocks private SeatServiceImpl service;

    private UUID buildingId;
    private UUID floorId;
    private UUID zoneId;
    private UUID seatId;
    private Floor floor;
    private Zone zone;
    private Seat seat;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        seatId = UUID.randomUUID();

        floor = Floor.builder()
                .id(floorId).buildingId(buildingId).name("G").floorNumber(0)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        zone = Zone.builder()
                .id(zoneId).floorId(floorId).buildingId(buildingId).name("Zone A")
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        seat = Seat.builder()
                .id(seatId).zoneId(zoneId).floorId(floorId).buildingId(buildingId)
                .seatNumber("A-01").seatType(SeatType.HOT_DESK)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private void stubFloorAndZone() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId))
                .thenReturn(Optional.of(floor));
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId))
                .thenReturn(Optional.of(zone));
    }

    @Test
    void listSeats_returnsActiveSeats() {
        stubFloorAndZone();
        when(seatRepository.findByZoneIdAndActiveTrueAndDeletedAtIsNull(zoneId)).thenReturn(List.of(seat));

        List<SeatResponse> result = service.listSeats(buildingId, floorId, zoneId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSeatNumber()).isEqualTo("A-01");
        assertThat(result.getFirst().getSeatType()).isEqualTo(SeatType.HOT_DESK);
        assertThat(result.getFirst().getZoneId()).isEqualTo(zoneId);
    }

    @Test
    void listSeats_floorNotFound_throwsFloorNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listSeats(buildingId, floorId, zoneId))
                .isInstanceOf(FloorNotFoundException.class);
    }

    @Test
    void listSeats_zoneNotFound_throwsZoneNotFoundException() {
        when(floorRepository.findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(floorId, buildingId))
                .thenReturn(Optional.of(floor));
        when(zoneRepository.findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(zoneId, floorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listSeats(buildingId, floorId, zoneId))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void getSeat_notFound_throwsSeatNotFoundException() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSeat(buildingId, floorId, zoneId, seatId))
                .isInstanceOf(SeatNotFoundException.class)
                .hasMessageContaining(seatId.toString());
    }

    @Test
    void createSeat_savesAndReturnsResponse() {
        stubFloorAndZone();
        when(seatRepository.existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(zoneId, "B-01")).thenReturn(false);
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> {
            Seat s = inv.getArgument(0);
            return Seat.builder()
                    .id(UUID.randomUUID()).zoneId(s.getZoneId()).floorId(s.getFloorId())
                    .buildingId(s.getBuildingId()).seatNumber(s.getSeatNumber())
                    .seatType(s.getSeatType()).xPosition(s.getXPosition()).yPosition(s.getYPosition())
                    .createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
        });

        CreateSeatRequest request = new CreateSeatRequest("B-01", SeatType.PERMANENT, 1.0f, 2.0f);
        SeatResponse result = service.createSeat(buildingId, floorId, zoneId, request);

        assertThat(result.getSeatNumber()).isEqualTo("B-01");
        assertThat(result.getSeatType()).isEqualTo(SeatType.PERMANENT);
        assertThat(result.getXPosition()).isEqualTo(1.0f);
        verify(seatRepository).save(argThat(s -> s.getSeatNumber().equals("B-01") && s.getZoneId().equals(zoneId)));
    }

    @Test
    void createSeat_duplicateNumber_throwsDuplicateSeatNumberException() {
        stubFloorAndZone();
        when(seatRepository.existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(zoneId, "A-01")).thenReturn(true);

        assertThatThrownBy(() -> service.createSeat(buildingId, floorId, zoneId, new CreateSeatRequest("A-01", SeatType.HOT_DESK, null, null)))
                .isInstanceOf(DuplicateSeatNumberException.class)
                .hasMessageContaining("A-01");
    }

    @Test
    void updateSeat_sameNumber_doesNotCheckDuplicate() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateSeat(buildingId, floorId, zoneId, seatId, new UpdateSeatRequest("A-01", SeatType.PERMANENT, null, null));

        verify(seatRepository, never()).existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(any(), any());
    }

    @Test
    void updateSeat_differentNumberThatExists_throwsDuplicateSeatNumberException() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.of(seat));
        when(seatRepository.existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(zoneId, "A-02")).thenReturn(true);

        assertThatThrownBy(() -> service.updateSeat(buildingId, floorId, zoneId, seatId, new UpdateSeatRequest("A-02", SeatType.HOT_DESK, null, null)))
                .isInstanceOf(DuplicateSeatNumberException.class)
                .hasMessageContaining("A-02");
    }

    @Test
    void updateSeat_differentNumberNotDuplicate_updatesSuccessfully() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.of(seat));
        when(seatRepository.existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(zoneId, "A-99")).thenReturn(false);
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        SeatResponse result = service.updateSeat(buildingId, floorId, zoneId, seatId, new UpdateSeatRequest("A-99", SeatType.PERMANENT, 3.0f, 4.0f));

        assertThat(result.getSeatNumber()).isEqualTo("A-99");
        assertThat(result.getSeatType()).isEqualTo(SeatType.PERMANENT);
    }

    @Test
    void deactivateSeat_setsInactive() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateSeat(buildingId, floorId, zoneId, seatId);

        verify(seatRepository).save(argThat(s -> !s.isActive()));
    }

    @Test
    void deactivateSeat_seatNotFound_throwsSeatNotFoundException() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateSeat(buildingId, floorId, zoneId, seatId))
                .isInstanceOf(SeatNotFoundException.class);
    }

    @Test
    void updateSeatStatus_changesStatus() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        SeatResponse result = service.updateSeatStatus(buildingId, floorId, zoneId, seatId, new UpdateSeatStatusRequest(SeatStatus.MAINTENANCE));

        assertThat(result.getStatus()).isEqualTo(SeatStatus.MAINTENANCE);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void updateSeatStatus_seatNotFound_throwsSeatNotFoundException() {
        stubFloorAndZone();
        when(seatRepository.findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(seatId, zoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSeatStatus(buildingId, floorId, zoneId, seatId, new UpdateSeatStatusRequest(SeatStatus.UNAVAILABLE)))
                .isInstanceOf(SeatNotFoundException.class);
    }
}
