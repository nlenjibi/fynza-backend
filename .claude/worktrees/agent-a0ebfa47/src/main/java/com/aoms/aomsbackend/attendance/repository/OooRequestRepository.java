package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.OooRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OooRequestRepository extends JpaRepository<OooRequest, UUID> {

    List<OooRequest> findByBuildingIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID buildingId, String status, LocalDate date, LocalDate dateToo);
}
