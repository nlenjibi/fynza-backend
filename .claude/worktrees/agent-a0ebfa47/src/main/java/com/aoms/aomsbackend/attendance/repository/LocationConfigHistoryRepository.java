package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.LocationConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocationConfigHistoryRepository extends JpaRepository<LocationConfigHistory, UUID> {
}
