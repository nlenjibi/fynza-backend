package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.BlockReservationResponse;
import com.aoms.aomsbackend.attendance.dto.CreateBlockReservationRequest;

import java.util.UUID;

/**
 * Manages block seat reservations created by managers for their teams.
 */
public interface BlockReservationService {

    /**
     * Creates a block reservation by selecting {@code request.seatCount} available seats
     * in the specified room and creating CONFIRMED placeholder bookings for each.
     *
     * @param managerId the UUID of the authenticated manager
     * @param request   the block reservation request details
     * @return the created block reservation with placeholder booking IDs
     * @throws com.aoms.aomsbackend.common.exception.NotFoundException   if the room does not exist
     * @throws com.aoms.aomsbackend.common.exception.ForbiddenException  if the caller lacks MANAGER role
     * @throws com.aoms.aomsbackend.common.exception.BadRequestException if fewer seats are available than requested
     */
    BlockReservationResponse createBlockReservation(UUID managerId, CreateBlockReservationRequest request);

    /**
     * Cancels an active block reservation. Placeholder bookings (where the user is the manager)
     * are cancelled; bookings already claimed by team members are preserved.
     *
     * @param managerId the UUID of the authenticated manager
     * @param blockId   the ID of the block reservation to cancel
     * @return the updated block reservation
     * @throws com.aoms.aomsbackend.common.exception.NotFoundException   if no block reservation exists with that ID
     * @throws com.aoms.aomsbackend.common.exception.ForbiddenException  if the caller is not the owning manager
     * @throws com.aoms.aomsbackend.common.exception.BadRequestException if the reservation is already cancelled
     */
    BlockReservationResponse cancelBlockReservation(UUID managerId, UUID blockId);
}
