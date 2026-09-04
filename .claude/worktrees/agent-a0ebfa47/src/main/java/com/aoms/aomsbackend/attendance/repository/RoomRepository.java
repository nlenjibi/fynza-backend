package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
}
