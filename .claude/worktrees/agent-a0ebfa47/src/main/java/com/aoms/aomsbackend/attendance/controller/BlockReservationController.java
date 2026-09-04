package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.BlockReservationResponse;
import com.aoms.aomsbackend.attendance.dto.CreateBlockReservationRequest;
import com.aoms.aomsbackend.attendance.service.BlockReservationService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for manager block seat reservations.
 *
 * <p>Authorisation is enforced in the service layer: the caller must hold the MANAGER role
 * for the building that contains the requested room.
 */
@RestController
@RequestMapping("/api/v1/block-reservations")
@RequiredArgsConstructor
@RequiresRole(UserRoleType.MANAGER)
@Tag(name = "Block Reservations", description = "Manager block seat reservations for collaboration days")
public class BlockReservationController {

    private final BlockReservationService blockReservationService;

    /**
     * Creates a block reservation.
     *
     * @param request         the reservation details
     * @param httpRequest     the HTTP request (used to read the authenticated manager's session)
     * @return 201 Created with the new block reservation
     */
    @PostMapping
    @Operation(
        summary = "Create a block reservation",
        description = "Reserves a contiguous block of seats in a room for a specific date. "
            + "The manager must hold the MANAGER role for the room's building. "
            + "Placeholder CONFIRMED bookings are created for each seat so the floor plan shows them as BOOKED.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Block reservation created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failed or insufficient available seats"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have MANAGER role for this building"),
                    @ApiResponse(responseCode = "404", description = "Room not found")
            }
    )
    public ResponseEntity<ResponseWrapper<BlockReservationResponse>> createBlockReservation(
            @RequestBody @Valid CreateBlockReservationRequest request,
            HttpServletRequest httpRequest) {

        UUID managerId = resolveUserId(httpRequest);
        BlockReservationResponse response = blockReservationService.createBlockReservation(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.success(response));
    }

    /**
     * Cancels an existing block reservation.
     *
     * @param id          the block reservation ID
     * @param httpRequest the HTTP request (used to read the authenticated manager's session)
     * @return 200 OK with the updated block reservation
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Cancel a block reservation",
        description = "Cancels an active block reservation. Placeholder bookings (seats not yet claimed "
            + "by team members) are cancelled. Bookings already claimed by employees are preserved.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Block reservation cancelled successfully"),
                    @ApiResponse(responseCode = "400", description = "Reservation is already cancelled"),
                    @ApiResponse(responseCode = "403", description = "Caller is not the owning manager"),
                    @ApiResponse(responseCode = "404", description = "Block reservation not found")
            }
    )
    public ResponseEntity<ResponseWrapper<BlockReservationResponse>> cancelBlockReservation(
            @Parameter(description = "Block reservation ID") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID managerId = resolveUserId(httpRequest);
        BlockReservationResponse response = blockReservationService.cancelBlockReservation(managerId, id);
        return ResponseEntity.ok(ResponseWrapper.success(response));
    }

    private UUID resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException();
        }
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            throw new SessionExpiredException();
        }
        return UUID.fromString(userId);
    }
}
