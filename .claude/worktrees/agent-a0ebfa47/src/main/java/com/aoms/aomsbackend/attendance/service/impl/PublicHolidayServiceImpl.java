package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.DeleteHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayCreateRequest;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayUpdateRequest;
import com.aoms.aomsbackend.attendance.entity.OfficeBuilding;
import com.aoms.aomsbackend.attendance.entity.PublicHoliday;
import com.aoms.aomsbackend.attendance.event.HolidayDeletedEvent;
import com.aoms.aomsbackend.attendance.repository.OfficeBuildingRepository;
import com.aoms.aomsbackend.attendance.repository.PublicHolidayRepository;
import com.aoms.aomsbackend.attendance.service.PublicHolidayService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link PublicHolidayService}.
 * Enforces business rules (role-based past-date guards, duplicate detection, ownership checks)
 * and coordinates persistence and event publishing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicHolidayServiceImpl implements PublicHolidayService {

    private final PublicHolidayRepository holidayRepository;
    private final OfficeBuildingRepository officeBuildingRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     * Past-date creation is blocked for HR; SUPER_ADMIN bypasses this guard.
     */
    @Override
    @Transactional
    public PublicHolidayResponse create(UUID locationId, PublicHolidayCreateRequest request,
                                        UUID createdBy, UserRoleType actingRole) {
        LocalDate holidayDate = request.getHolidayDate();

        if (holidayDate.isBefore(LocalDate.now()) && actingRole != UserRoleType.SUPER_ADMIN) {
            throw new BadRequestException("Cannot create a public holiday in the past");
        }

        if (holidayRepository.findByBuildingIdAndHolidayDate(locationId, holidayDate).isPresent()) {
            throw new ConflictException(
                    "A public holiday already exists for this location on " + holidayDate,
                    "HOLIDAY_ALREADY_EXISTS");
        }

        PublicHoliday holiday = new PublicHoliday();
        holiday.setBuildingId(locationId);
        holiday.setHolidayDate(holidayDate);
        holiday.setName(request.getName());
        holiday.setCreatedBy(createdBy);

        PublicHoliday saved = holidayRepository.save(holiday);

        log.info("Public holiday created: id={}, locationId={}, date={}", saved.getId(), locationId, holidayDate);
        return toResponse(saved);
    }

    /**
     * {@inheritDoc}
     * When {@code year} is provided, only holidays in that calendar year are returned.
     */
    @Override
    public List<PublicHolidayResponse> list(UUID locationId, Integer year) {
        List<PublicHoliday> holidays;
        if (year == null) {
            holidays = holidayRepository.findByBuildingIdOrderByHolidayDateAsc(locationId);
        } else {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            holidays = holidayRepository
                    .findByBuildingIdAndHolidayDateBetweenOrderByHolidayDateAsc(locationId, start, end);
        }
        return holidays.stream().map(this::toResponse).toList();
    }

    /**
     * {@inheritDoc}
     * Only future holidays may be updated. Past holidays are immutable.
     * At least one of {@code holidayDate} or {@code name} must be provided.
     */
    @Override
    @Transactional
    public PublicHolidayResponse update(UUID locationId, UUID holidayId, PublicHolidayUpdateRequest request) {
        if (request.getHolidayDate() == null && request.getName() == null) {
            throw new BadRequestException("At least one of holidayDate or name must be provided");
        }

        PublicHoliday holiday = loadAndVerify(locationId, holidayId);

        if (holiday.getHolidayDate().isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "Cannot modify a past public holiday", "PAST_HOLIDAY_IMMUTABLE");
        }

        if (request.getHolidayDate() != null
                && !request.getHolidayDate().equals(holiday.getHolidayDate())) {
            boolean duplicate = holidayRepository
                    .findByBuildingIdAndHolidayDate(locationId, request.getHolidayDate())
                    .isPresent();
            if (duplicate) {
                throw new ConflictException(
                        "A public holiday already exists for this location on " + request.getHolidayDate(),
                        "HOLIDAY_ALREADY_EXISTS");
            }
            holiday.setHolidayDate(request.getHolidayDate());
        }

        if (request.getName() != null) {
            holiday.setName(request.getName());
        }

        PublicHoliday saved = holidayRepository.save(holiday);

        log.info("Public holiday updated: id={}, locationId={}", saved.getId(), locationId);
        return toResponse(saved);
    }

    /**
     * {@inheritDoc}
     * Past-holiday deletion is restricted to SUPER_ADMIN and triggers a Pass 2 restamp
     * via {@link HolidayDeletedEvent} (fired after the transaction commits).
     * Future-holiday deletion is open to HR and SUPER_ADMIN and does not trigger a restamp.
     * Throws {@link IllegalStateException} if no {@code OfficeBuilding} exists for the location,
     * which rolls back the entire transaction.
     */
    @Override
    @Transactional
    public DeleteHolidayResponse delete(UUID locationId, UUID holidayId, UserRoleType actingRole) {
        PublicHoliday holiday = loadAndVerify(locationId, holidayId);
        boolean isPast = holiday.getHolidayDate().isBefore(LocalDate.now());

        LocalDate deletedDate = holiday.getHolidayDate();

        if (isPast && actingRole != UserRoleType.SUPER_ADMIN) {
            throw new ForbiddenException();
        }

        if (isPast) {
            UUID officeId = officeBuildingRepository.findById(locationId)
                    .map(OfficeBuilding::getOfficeId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No OfficeBuilding found for locationId: " + locationId));

            holidayRepository.delete(holiday);

            eventPublisher.publishEvent(new HolidayDeletedEvent(locationId, officeId, deletedDate));

            log.info("Past holiday deleted, restamp queued: id={}, date={}", holidayId, deletedDate);
            return DeleteHolidayResponse.builder().restampQueued(true).build();
        }

        holidayRepository.delete(holiday);

        log.info("Future holiday deleted: id={}, date={}", holidayId, deletedDate);
        return DeleteHolidayResponse.builder().restampQueued(false).build();
    }

    /**
     * Loads a holiday by ID and verifies it belongs to the given location.
     * Throws {@link NotFoundException} if not found or if the location does not match.
     */
    private PublicHoliday loadAndVerify(UUID locationId, UUID holidayId) {
        PublicHoliday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new NotFoundException("Public holiday not found: " + holidayId));
        if (!holiday.getBuildingId().equals(locationId)) {
            throw new NotFoundException("Public holiday not found: " + holidayId);
        }
        return holiday;
    }

    /** Maps a {@link PublicHoliday} entity to its response DTO, aliasing {@code buildingId} as {@code locationId}. */
    private PublicHolidayResponse toResponse(PublicHoliday holiday) {
        return PublicHolidayResponse.builder()
                .id(holiday.getId())
                .locationId(holiday.getBuildingId())
                .holidayDate(holiday.getHolidayDate())
                .name(holiday.getName())
                .createdBy(holiday.getCreatedBy())
                .createdAt(holiday.getCreatedAt())
                .build();
    }
}
