package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.OfficeBuilding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link OfficeBuilding} records.
 *
 * <p>Marked read-only in practice — buildings are managed by the data-engineering pipeline,
 * not by this service.
 */
@Repository
public interface OfficeBuildingRepository extends JpaRepository<OfficeBuilding, UUID> {

    /**
     * Returns all buildings that are currently active ({@code is_active = true}).
     * Used by {@code NoShowAutoReleaseJob} to enumerate locations that need processing.
     *
     * @return list of active buildings, possibly empty
     */
    List<OfficeBuilding> findByActiveTrue();

    /**
     * Resolves the IANA timezone string for a building by traversing the hierarchy
     * {@code office_building → office → organisation}.
     *
     * <p>The timezone lives on {@code organisation} because all buildings in the same
     * organisation share a country/timezone. The join is performed as a single native
     * query to avoid loading the full entity graph.
     *
     * @param buildingId the building whose timezone is needed
     * @return the timezone string (e.g. {@code "Africa/Accra"}), or empty if the
     *         building has no parent organisation row
     */
    @Query(value = """
            SELECT o.timezone
            FROM organisation o
            JOIN office off ON off.organisation_id = o.id
            JOIN office_building ob ON ob.office_id = off.id
            WHERE ob.id = :buildingId
            """, nativeQuery = true)
    Optional<String> findTimezoneByBuildingId(@Param("buildingId") UUID buildingId);
}
