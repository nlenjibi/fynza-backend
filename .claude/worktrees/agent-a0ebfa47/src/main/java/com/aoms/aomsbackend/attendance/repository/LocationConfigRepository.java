package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * The interface Location config repository.
 */
@Repository
public interface LocationConfigRepository extends JpaRepository<LocationConfig, UUID> {

    /**
     * Find by building id optional.
     *
     * @param buildingId the building id
     * @return the optional
     */
    Optional<LocationConfig> findByBuildingId(UUID buildingId);
}
