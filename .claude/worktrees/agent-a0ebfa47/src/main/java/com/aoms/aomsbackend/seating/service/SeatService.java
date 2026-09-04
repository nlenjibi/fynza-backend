package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.request.CreateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatStatusRequest;
import com.aoms.aomsbackend.seating.dto.response.SeatResponse;

import java.util.List;
import java.util.UUID;

/**
 * The interface Seat service.
 */
public interface SeatService {

    /**
     * List seats list.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @return the list
     */
    List<SeatResponse> listSeats(UUID buildingId, UUID floorId, UUID zoneId);

    /**
     * Gets seat.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @param seatId     the seat id
     * @return the seat
     */
    SeatResponse getSeat(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId);

    /**
     * Create seat seat response.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @param request    the request
     * @return the seat response
     */
    SeatResponse createSeat(UUID buildingId, UUID floorId, UUID zoneId, CreateSeatRequest request);

    /**
     * Update seat seat response.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @param seatId     the seat id
     * @param request    the request
     * @return the seat response
     */
    SeatResponse updateSeat(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId, UpdateSeatRequest request);

    /**
     * Deactivate seat.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @param seatId     the seat id
     */
    void deactivateSeat(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId);

    /**
     * Update seat status seat response.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @param seatId     the seat id
     * @param request    the request
     * @return the seat response
     */
    SeatResponse updateSeatStatus(UUID buildingId, UUID floorId, UUID zoneId, UUID seatId, UpdateSeatStatusRequest request);
}
