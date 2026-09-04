package com.aoms.aomsbackend.auth.repository;

import com.aoms.aomsbackend.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * The interface User role repository.
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Find by user id list.
     *
     * @param userId the user id
     * @return the list
     */
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<UserRole> findByOrganisationIdAndDeletedAtIsNull(UUID organisationId);
}
