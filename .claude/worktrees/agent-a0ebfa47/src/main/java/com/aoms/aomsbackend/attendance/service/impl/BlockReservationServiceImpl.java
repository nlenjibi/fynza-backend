package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.BlockReservationResponse;
import com.aoms.aomsbackend.attendance.dto.CreateBlockReservationRequest;
import com.aoms.aomsbackend.attendance.entity.*;
import com.aoms.aomsbackend.attendance.repository.*;
import com.aoms.aomsbackend.attendance.service.BlockReservationService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockReservationServiceImpl implements BlockReservationService {

    private final BlockReservationRepository blockReservationRepository;
    private final SeatBookingRepository seatBookingRepository;
    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;
    private final UserRoleAccessService userRoleAccessService;

    @Override
    @Transactional
    public BlockReservationResponse createBlockReservation(UUID managerId, CreateBlockReservationRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found: " + request.getRoomId()));

        UUID buildingId = room.getBuildingId();

        if (!userRoleAccessService.hasAccess(managerId, buildingId, UserRoleType.MANAGER)) {
            throw new ForbiddenException();
        }

        // Native query returns VARCHAR strings for H2/PostgreSQL JDBC compatibility
        List<UUID> availableSeatIds = seatRepository
                .selectAvailableSeatsForUpdate(request.getRoomId(), request.getReservationDate(), request.getSeatCount())
                .stream().map(UUID::fromString).toList();

        if (availableSeatIds.size() < request.getSeatCount()) {
            throw new BadRequestException(
                    "Insufficient available seats: requested " + request.getSeatCount()
                    + ", available " + availableSeatIds.size());
        }

        BlockReservation block = BlockReservation.builder()
                .managerId(managerId)
                .buildingId(buildingId)
                .roomId(request.getRoomId())
                .reservationDate(request.getReservationDate())
                .seatCount(request.getSeatCount())
                .notes(request.getNotes())
                .status(BlockReservationStatus.ACTIVE)
                .build();
        block = blockReservationRepository.save(block);

        final UUID blockId = block.getId();
        List<SeatBooking> placeholders = availableSeatIds.stream()
                .map(seatId -> SeatBooking.builder()
                        .seatId(seatId)
                        .userId(managerId)
                        .buildingId(buildingId)
                        .bookingDate(request.getReservationDate())
                        .status(SeatBookingStatus.CONFIRMED)
                        .blockReservationId(blockId)
                        .build())
                .toList();
        List<SeatBooking> saved = seatBookingRepository.saveAll(placeholders);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(managerId)
                .actorRole(UserRoleType.MANAGER)
                .action("BLOCK_RESERVATION_CREATED")
                .entityType("BlockReservation")
                .entityId(blockId)
                .locationId(buildingId)
                .build());

        log.info("Block reservation {} created by manager {} for {} seats on {}",
                blockId, managerId, request.getSeatCount(), request.getReservationDate());

        return toResponse(block, saved.stream().map(SeatBooking::getId).toList());
    }

    @Override
    @Transactional
    public BlockReservationResponse cancelBlockReservation(UUID managerId, UUID blockId) {
        BlockReservation block = blockReservationRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Block reservation not found: " + blockId));

        if (!block.getManagerId().equals(managerId)) {
            throw new ForbiddenException();
        }

        if (block.getStatus() == BlockReservationStatus.CANCELLED) {
            throw new BadRequestException("Block reservation is already cancelled");
        }

        List<SeatBooking> bookings = seatBookingRepository.findByBlockReservationId(blockId);
        OffsetDateTime now = OffsetDateTime.now();

        for (SeatBooking booking : bookings) {
            if (booking.getUserId().equals(managerId)) {
                // Placeholder — cancel it
                booking.setStatus(SeatBookingStatus.CANCELLED);
                booking.setCancelledAt(now);
                booking.setCancellationReason("Block reservation cancelled");
                seatBookingRepository.save(booking);
            }
            // Employee-claimed bookings are left untouched
        }

        block.setStatus(BlockReservationStatus.CANCELLED);
        block = blockReservationRepository.save(block);

        auditLogService.log(AuditLogEntry.builder()
                .actorId(managerId)
                .actorRole(UserRoleType.MANAGER)
                .action("BLOCK_RESERVATION_CANCELLED")
                .entityType("BlockReservation")
                .entityId(blockId)
                .locationId(block.getBuildingId())
                .build());

        log.info("Block reservation {} cancelled by manager {}", blockId, managerId);

        List<UUID> remainingBookingIds = bookings.stream()
                .map(SeatBooking::getId)
                .toList();
        return toResponse(block, remainingBookingIds);
    }

    private BlockReservationResponse toResponse(BlockReservation block, List<UUID> bookingIds) {
        return BlockReservationResponse.builder()
                .id(block.getId())
                .managerId(block.getManagerId())
                .buildingId(block.getBuildingId())
                .roomId(block.getRoomId())
                .reservationDate(block.getReservationDate())
                .seatCount(block.getSeatCount())
                .notes(block.getNotes())
                .status(block.getStatus())
                .seatBookingIds(bookingIds)
                .createdAt(block.getCreatedAt())
                .build();
    }
}
