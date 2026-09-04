package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.LocationConfigResponse;
import com.aoms.aomsbackend.attendance.dto.LocationConfigUpdateRequest;

import java.util.UUID;

/**
 * The interface Location config service.
 */
public interface LocationConfigService {

    /**
     * Gets by building id.
     *
     * @param buildingId the building id
     * @return the by building id
     */
    LocationConfigResponse getByBuildingId(UUID buildingId);

    /**
     * Update by building id location config response.
     *
     * @param buildingId the building id
     * @param request    the request
     * @return the location config response
     */
    LocationConfigResponse updateByBuildingId(UUID buildingId, LocationConfigUpdateRequest request);

    LocationConfigResponse updateSeatVisibility(UUID buildingId, UpdateSeatVisibilityRequest request, UUID actorId);
}
