package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PublicHoliday} entities.
 * Used by AttendancePass2Service to check for public holidays during overlay,
 * and by PublicHolidayService for full CRUD management.
 */
@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    /**
     * Returns a single holiday matching the given building and date, used by Pass 2 and duplicate checks.
     *
     * @param buildingId  the location (building) UUID
     * @param holidayDate the specific date to check
     * @return an Optional containing the holiday if found
     */
    Optional<PublicHoliday> findByBuildingIdAndHolidayDate(UUID buildingId, LocalDate holidayDate);

    /**
     * Returns all holidays for a location, sorted chronologically ascending.
     *
     * @param buildingId the location (building) UUID
     * @return list of holidays ordered by date ASC
     */
    List<PublicHoliday> findByBuildingIdOrderByHolidayDateAsc(UUID buildingId);

    /**
     * Returns holidays for a location within the given date range, sorted chronologically ascending.
     * Used to filter holidays by year when the {@code ?year} query param is provided.
     *
     * @param buildingId the location (building) UUID
     * @param start      inclusive start date (e.g. Jan 1 of the year)
     * @param end        inclusive end date (e.g. Dec 31 of the year)
     * @return list of holidays in range ordered by date ASC
     */
    List<PublicHoliday> findByBuildingIdAndHolidayDateBetweenOrderByHolidayDateAsc(
            UUID buildingId, LocalDate start, LocalDate end);
}
