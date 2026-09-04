package com.aoms.aomsbackend.auth.repository;

import com.aoms.aomsbackend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findBySsoUserId(String ssoUserId);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeId(String employeeId);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.deletedAt IS NULL AND " +
           "EXISTS (SELECT 1 FROM UserRole ur WHERE ur.userId = u.id AND ur.organisationId = :organisationId AND ur.deletedAt IS NULL)")
    List<User> findActiveUsersByOrganisation(@Param("organisationId") UUID organisationId);
}
