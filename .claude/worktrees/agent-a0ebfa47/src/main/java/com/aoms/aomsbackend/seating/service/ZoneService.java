package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.request.CreateZoneRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateZoneRequest;
import com.aoms.aomsbackend.seating.dto.response.ZoneResponse;

import java.util.List;
import java.util.UUID;

/**
 * The interface Zone service.
 */
public interface ZoneService {

    /**
     * List zones list.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @return the list
     */
    List<ZoneResponse> listZones(UUID buildingId, UUID floorId);

    /**
     * Gets zone.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @return the zone
     */
    ZoneResponse getZone(UUID buildingId, UUID floorId, UUID zoneId);

    /**
     * Create zone zone response.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param request    the request
     * @return the zone response
     */
    ZoneResponse createZone(UUID buildingId, UUID floorId, CreateZoneRequest request);

    /**
     * Update zone zone response.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     * @param request    the request
     * @return the zone response
     */
    ZoneResponse updateZone(UUID buildingId, UUID floorId, UUID zoneId, UpdateZoneRequest request);

    /**
     * Deactivate zone.
     *
     * @param buildingId the building id
     * @param floorId    the floor id
     * @param zoneId     the zone id
     */
    void deactivateZone(UUID buildingId, UUID floorId, UUID zoneId);
}
