package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.attendance.dto.CreateSeatBookingRequest;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.service.impl.SeatBookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatBookingServiceImplTest {

    @Mock private SeatBookingRepository seatBookingRepository;

    @InjectMocks
    private SeatBookingServiceImpl service;

    private static final UUID EMPLOYEE_ID  = UUID.randomUUID();
    private static final UUID MANAGER_ID   = UUID.randomUUID();
    private static final UUID SEAT_ID      = UUID.randomUUID();
    private static final UUID BUILDING_ID  = UUID.randomUUID();
    private static final UUID BLOCK_ID     = UUID.randomUUID();
    private static final LocalDate DATE    = LocalDate.now().plusDays(3);

    // ── Team member claims a block seat ──────────────────────────────────────

    @Test
    void createSeatBooking_withinBlock_updatesPlaceholderInPlace() {
        UUID bookingId = UUID.randomUUID();
        SeatBooking placeholder = placeholder(bookingId);

        when(seatBookingRepository
                .findBySeatIdAndBookingDateAndStatusAndBlockReservationIdIsNotNull(
                        SEAT_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(Optional.of(placeholder));
        when(seatBookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SeatBookingResponse response = service.createSeatBooking(EMPLOYEE_ID, request());

        // userId must be updated to the employee
        verify(seatBookingRepository).save(argThat(b ->
                bookingId.equals(b.getId()) &&
                EMPLOYEE_ID.equals(b.getUserId()) &&
                BLOCK_ID.equals(b.getBlockReservationId())));

        assertThat(response.getUserId()).isEqualTo(EMPLOYEE_ID);
        assertThat(response.getBlockReservationId()).isEqualTo(BLOCK_ID);

        // No new record created
        verify(seatBookingRepository, never()).save(argThat(b -> b.getId() == null));
    }

    // ── Regular booking when no block placeholder exists ─────────────────────

    @Test
    void createSeatBooking_regularBooking_createsNewRecord() {
        when(seatBookingRepository
                .findBySeatIdAndBookingDateAndStatusAndBlockReservationIdIsNotNull(
                        SEAT_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(Optional.empty());
        when(seatBookingRepository.existsBySeatIdAndBookingDateAndStatus(
                SEAT_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(false);

        SeatBooking saved = newBooking(UUID.randomUUID());
        when(seatBookingRepository.save(any())).thenReturn(saved);

        SeatBookingResponse response = service.createSeatBooking(EMPLOYEE_ID, request());

        verify(seatBookingRepository).save(argThat(b ->
                EMPLOYEE_ID.equals(b.getUserId()) &&
                SEAT_ID.equals(b.getSeatId()) &&
                b.getBlockReservationId() == null &&
                b.getStatus() == SeatBookingStatus.CONFIRMED));

        assertThat(response.getBlockReservationId()).isNull();
    }

    // ── Seat already booked → 400 ─────────────────────────────────────────────

    @Test
    void createSeatBooking_seatAlreadyBooked_throws400() {
        when(seatBookingRepository
                .findBySeatIdAndBookingDateAndStatusAndBlockReservationIdIsNotNull(
                        SEAT_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(Optional.empty());
        when(seatBookingRepository.existsBySeatIdAndBookingDateAndStatus(
                SEAT_ID, DATE, SeatBookingStatus.CONFIRMED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createSeatBooking(EMPLOYEE_ID, request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already booked");

        verify(seatBookingRepository, never()).save(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CreateSeatBookingRequest request() {
        CreateSeatBookingRequest req = new CreateSeatBookingRequest();
        req.setSeatId(SEAT_ID);
        req.setBookingDate(DATE);
        req.setBuildingId(BUILDING_ID);
        return req;
    }

    private SeatBooking placeholder(UUID id) {
        SeatBooking b = new SeatBooking();
        b.setId(id);
        b.setSeatId(SEAT_ID);
        b.setUserId(MANAGER_ID);
        b.setBuildingId(BUILDING_ID);
        b.setBookingDate(DATE);
        b.setStatus(SeatBookingStatus.CONFIRMED);
        b.setBlockReservationId(BLOCK_ID);
        b.setCreatedAt(OffsetDateTime.now());
        return b;
    }

    private SeatBooking newBooking(UUID id) {
        SeatBooking b = new SeatBooking();
        b.setId(id);
        b.setSeatId(SEAT_ID);
        b.setUserId(EMPLOYEE_ID);
        b.setBuildingId(BUILDING_ID);
        b.setBookingDate(DATE);
        b.setStatus(SeatBookingStatus.CONFIRMED);
        b.setCreatedAt(OffsetDateTime.now());
        return b;
    }
}
