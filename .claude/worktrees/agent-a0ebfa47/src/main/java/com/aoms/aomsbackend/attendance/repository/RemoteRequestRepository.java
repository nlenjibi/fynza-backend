package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.RemoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RemoteRequestRepository extends JpaRepository<RemoteRequest, UUID> {

    List<RemoteRequest> findByBuildingIdAndStatusAndRequestDate(
            UUID buildingId, String status, LocalDate requestDate);
}
