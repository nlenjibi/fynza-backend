package com.aoms.aomsbackend.seating.repository;

import com.aoms.aomsbackend.seating.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Floor repository.
 */
public interface FloorRepository extends JpaRepository<Floor, UUID> {

    /**
     * Find by building id and active true list.
     *
     * @param buildingId the building id
     * @return the list
     */
    List<Floor> findByBuildingIdAndActiveTrueAndDeletedAtIsNull(UUID buildingId);

    List<Floor> findByBuildingIdAndActiveTrueAndDeletedAtIsNullOrderByFloorNumber(UUID buildingId);

    /**
     * Find by id and building id and active true optional.
     *
     * @param id         the id
     * @param buildingId the building id
     * @return the optional
     */
    Optional<Floor> findByIdAndBuildingIdAndActiveTrueAndDeletedAtIsNull(UUID id, UUID buildingId);
}
