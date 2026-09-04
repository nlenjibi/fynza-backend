package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.AssignPermanentSeatRequest;
import com.aoms.aomsbackend.seating.dto.SeatAssignmentResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface SeatManagementService {

    SeatAssignmentResponse assign(UUID seatId, AssignPermanentSeatRequest req, HttpServletRequest request);

    SeatAssignmentResponse unassign(UUID seatId, HttpServletRequest request);

    SeatTypeResponse convertType(UUID seatId, SeatTypeUpdateRequest req, HttpServletRequest request);
}
