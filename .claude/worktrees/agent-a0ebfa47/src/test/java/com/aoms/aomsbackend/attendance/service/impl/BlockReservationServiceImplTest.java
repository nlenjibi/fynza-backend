package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.BlockReservationResponse;
import com.aoms.aomsbackend.attendance.dto.CreateBlockReservationRequest;
import com.aoms.aomsbackend.attendance.entity.*;
import com.aoms.aomsbackend.attendance.repository.BlockReservationRepository;
import com.aoms.aomsbackend.attendance.repository.RoomRepository;
import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockReservationServiceImplTest {

    @Mock private BlockReservationRepository blockReservationRepository;
    @Mock private SeatBookingRepository seatBookingRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRoleAccessService userRoleAccessService;

    @InjectMocks
    private BlockReservationServiceImpl service;

    private static final UUID MANAGER_ID   = UUID.randomUUID();
    private static final UUID BUILDING_ID  = UUID.randomUUID();
    private static final UUID ROOM_ID      = UUID.randomUUID();
    private static final UUID BLOCK_ID     = UUID.randomUUID();
    private static final LocalDate DATE    = LocalDate.now().plusDays(7);

    private Room room;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id(ROOM_ID)
                .buildingId(BUILDING_ID)
                .floorId(UUID.randomUUID())
                .roomName("Collab Room A")
                .roomType("OPEN_OFFICE")
                .active(true)
                .build();
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(userRoleAccessService.hasAccess(MANAGER_ID, BUILDING_ID, UserRoleType.MANAGER)).thenReturn(true);
    }

    // ── Successful block creation ────────────────────────────────────────────

    @Test
    void createBlockReservation_success_createsBlockAndPlaceholderBookingsAndAuditLog() {
        UUID seat1 = UUID.randomUUID();
        UUID seat2 = UUID.randomUUID();
        UUID seat3 = UUID.randomUUID();

        when(seatRepository.selectAvailableSeatsForUpdate(ROOM_ID, DATE, 3))
                .thenReturn(List.of(seat1.toString(), seat2.toString(), seat3.toString()));

        BlockReservation savedBlock = savedBlock(BLOCK_ID);
        when(blockReservationRepository.save(any(BlockReservation.class))).thenReturn(savedBlock);

        List<SeatBooking> savedBookings = List.of(
                bookingWith(UUID.randomUUID(), seat1, MANAGER_ID, BLOCK_ID),
                bookingWith(UUID.randomUUID(), seat2, MANAGER_ID, BLOCK_ID),
                bookingWith(UUID.randomUUID(), seat3, MANAGER_ID, BLOCK_ID));
        when(seatBookingRepository.saveAll(any())).thenReturn(savedBookings);

        BlockReservationResponse response = service.createBlockReservation(MANAGER_ID, request(3));

        assertThat(response.getId()).isEqualTo(BLOCK_ID);
        assertThat(response.getStatus()).isEqualTo(BlockReservationStatus.ACTIVE);
        assertThat(response.getSeatBookingIds()).hasSize(3);

        verify(seatBookingRepository).saveAll(argThat(bookings -> {
            List<SeatBooking> list = (List<SeatBooking>) bookings;
            return list.size() == 3 &&
                   list.stream().allMatch(b ->
                           b.getStatus() == SeatBookingStatus.CONFIRMED &&
                           b.getBlockReservationId().equals(BLOCK_ID) &&
                           b.getUserId().equals(MANAGER_ID));
        }));

        verify(auditLogService).log(argThat((AuditLogEntry entry) ->
                "BLOCK_RESERVATION_CREATED".equals(entry.getAction()) &&
                "BlockReservation".equals(entry.getEntityType()) &&
                BLOCK_ID.equals(entry.getEntityId()) &&
                MANAGER_ID.equals(entry.getActorId())));
    }

    // ── Insufficient seats → 400 ─────────────────────────────────────────────

    @Test
    void createBlockReservation_insufficientSeats_throws400() {
        when(seatRepository.selectAvailableSeatsForUpdate(ROOM_ID, DATE, 5))
                .thenReturn(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())); // only 2 available

        assertThatThrownBy(() -> service.createBlockReservation(MANAGER_ID, request(5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient available seats");

        verify(blockReservationRepository, never()).save(any());
    }

    // ── Non-manager role → 403 ───────────────────────────────────────────────

    @Test
    void createBlockReservation_notManager_throws403() {
        when(userRoleAccessService.hasAccess(MANAGER_ID, BUILDING_ID, UserRoleType.MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> service.createBlockReservation(MANAGER_ID, request(2)))
                .isInstanceOf(ForbiddenException.class);

        verify(seatRepository, never()).selectAvailableSeatsForUpdate(any(), any(), anyInt());
    }

    // ── Room not found → 404 ─────────────────────────────────────────────────

    @Test
    void createBlockReservation_roomNotFound_throws404() {
        UUID unknownRoom = UUID.randomUUID();
        when(roomRepository.findById(unknownRoom)).thenReturn(Optional.empty());

        CreateBlockReservationRequest req = request(2);
        req.setRoomId(unknownRoom);

        assertThatThrownBy(() -> service.createBlockReservation(MANAGER_ID, req))
                .isInstanceOf(NotFoundException.class);
    }

    // ── Cancel: placeholders cancelled, employee bookings preserved ──────────

    @Test
    void cancelBlockReservation_cancelsPlaceholders_preservesEmployeeBookings() {
        UUID employeeId = UUID.randomUUID();
        UUID placeholderBookingId = UUID.randomUUID();
        UUID claimedBookingId = UUID.randomUUID();

        BlockReservation block = savedBlock(BLOCK_ID);
        when(blockReservationRepository.findById(BLOCK_ID)).thenReturn(Optional.of(block));
        when(blockReservationRepository.save(any())).thenReturn(block);

        SeatBooking placeholder = bookingWith(placeholderBookingId, UUID.randomUUID(), MANAGER_ID, BLOCK_ID);
        SeatBooking claimed = bookingWith(claimedBookingId, UUID.randomUUID(), employeeId, BLOCK_ID);
        when(seatBookingRepository.findByBlockReservationId(BLOCK_ID)).thenReturn(List.of(placeholder, claimed));

        service.cancelBlockReservation(MANAGER_ID, BLOCK_ID);

        // Placeholder must be cancelled
        verify(seatBookingRepository).save(argThat(b ->
                placeholderBookingId.equals(b.getId()) &&
                b.getStatus() == SeatBookingStatus.CANCELLED &&
                b.getCancelledAt() != null));

        // Claimed booking must NOT be cancelled (save not called for it)
        verify(seatBookingRepository, never()).save(argThat(b ->
                claimedBookingId.equals(b.getId()) && b.getStatus() == SeatBookingStatus.CANCELLED));

        verify(auditLogService).log(argThat((AuditLogEntry entry) ->
                "BLOCK_RESERVATION_CANCELLED".equals(entry.getAction())));
    }

    // ── Cancel already-cancelled block → 400 ─────────────────────────────────

    @Test
    void cancelBlockReservation_alreadyCancelled_throws400() {
        BlockReservation block = savedBlock(BLOCK_ID);
        block.setStatus(BlockReservationStatus.CANCELLED);
        when(blockReservationRepository.findById(BLOCK_ID)).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> service.cancelBlockReservation(MANAGER_ID, BLOCK_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    // ── Cancel by non-owner → 403 ─────────────────────────────────────────────

    @Test
    void cancelBlockReservation_notOwner_throws403() {
        UUID otherManager = UUID.randomUUID();
        BlockReservation block = savedBlock(BLOCK_ID);
        when(blockReservationRepository.findById(BLOCK_ID)).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> service.cancelBlockReservation(otherManager, BLOCK_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CreateBlockReservationRequest request(int seats) {
        CreateBlockReservationRequest req = new CreateBlockReservationRequest();
        req.setRoomId(ROOM_ID);
        req.setReservationDate(DATE);
        req.setSeatCount(seats);
        req.setNotes("Team collab day");
        return req;
    }

    private BlockReservation savedBlock(UUID id) {
        return BlockReservation.builder()
                .id(id)
                .managerId(MANAGER_ID)
                .buildingId(BUILDING_ID)
                .roomId(ROOM_ID)
                .reservationDate(DATE)
                .seatCount(3)
                .status(BlockReservationStatus.ACTIVE)
                .build();
    }

    private SeatBooking bookingWith(UUID id, UUID seatId, UUID userId, UUID blockId) {
        SeatBooking b = new SeatBooking();
        b.setId(id);
        b.setSeatId(seatId);
        b.setUserId(userId);
        b.setBuildingId(BUILDING_ID);
        b.setBookingDate(DATE);
        b.setStatus(SeatBookingStatus.CONFIRMED);
        b.setBlockReservationId(blockId);
        return b;
    }
}
