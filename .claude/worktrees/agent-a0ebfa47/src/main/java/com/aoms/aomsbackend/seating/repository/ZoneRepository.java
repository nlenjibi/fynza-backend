package com.aoms.aomsbackend.seating.repository;

import com.aoms.aomsbackend.seating.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Zone repository.
 */
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    /**
     * Find by floor id and active true list.
     *
     * @param floorId the floor id
     * @return the list
     */
    List<Zone> findByFloorIdAndActiveTrueAndDeletedAtIsNull(UUID floorId);

    /**
     * Find by id and floor id and active true optional.
     *
     * @param id      the id
     * @param floorId the floor id
     * @return the optional
     */
    Optional<Zone> findByIdAndFloorIdAndActiveTrueAndDeletedAtIsNull(UUID id, UUID floorId);

    /**
     * Deactivate all by floor id.
     *
     * @param floorId the floor id
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE zone SET is_active = false, deleted_at = CURRENT_TIMESTAMP WHERE floor_id = :floorId AND is_active = true AND deleted_at IS NULL", nativeQuery = true)
    void deactivateAllByFloorId(@Param("floorId") UUID floorId);
}
