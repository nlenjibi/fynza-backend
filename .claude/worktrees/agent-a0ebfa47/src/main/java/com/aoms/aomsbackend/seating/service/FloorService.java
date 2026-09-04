package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.request.CreateFloorRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateFloorRequest;
import com.aoms.aomsbackend.seating.dto.response.FloorResponse;

import java.util.List;
import java.util.UUID;

/**
 * The interface Floor service.
 */
public interface FloorService {

    /**
     * List floors list.
     *
     * @param buildingId the building id
     * @return the list
     */
    List<FloorResponse> listFloors(UUID buildingId);

    /**
     * Gets floor.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @return the floor
     */
    FloorResponse getFloor(UUID buildingId, UUID floorId);

    /**
     * Create floor floor response.
     *
     * @param buildingId the building id
     * @param request    the request
     * @return the floor response
     */
    FloorResponse createFloor(UUID buildingId, CreateFloorRequest request);

    /**
     * Update floor floor response.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param request    the request
     * @return the floor response
     */
    FloorResponse updateFloor(UUID buildingId, UUID floorId, UpdateFloorRequest request);

    /**
     * Deactivate floor.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     */
    void deactivateFloor(UUID buildingId, UUID floorId);
}
