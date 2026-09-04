package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the read-only {@link Employee} table managed by the data-engineering pipeline.
 */
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    /**
     * Finds an employee by their SSO user ID, used to link the auth {@code User}
     * to the attendance {@code Employee} record.
     *
     * @param ssoUserId the SSO user identifier from the identity provider
     * @return the matching employee, or empty if not found
     */
    Optional<Employee> findBySsoUserId(String ssoUserId);

    /**
     * Returns all active, non-deleted direct reports of a manager at a specific building.
     * Used to enforce the location-scoped direct-report boundary.
     *
     * @param managerId        the manager's employee UUID
     * @param primaryBuildingId the building to scope the query to
     * @return list of active direct reports at that building
     */
    List<Employee> findByManagerIdAndPrimaryBuildingIdAndActiveTrueAndDeletedAtIsNull(
            UUID managerId, UUID primaryBuildingId);


    /**
     * Returns active, non-deleted employee IDs for a building, optionally filtered by department.
     * This avoids materializing full Employee entities when only identifiers are needed for pagination.
     *
     * @param primaryBuildingId the building UUID
     * @param department        the department name, or null to include all departments
     * @return matching employee IDs
     */
    @Query("SELECT e.id FROM Employee e"
            + " WHERE e.primaryBuildingId = :primaryBuildingId"
            + " AND e.active = true"
            + " AND e.deletedAt IS NULL"
            + " AND (:department IS NULL OR e.department = :department)")
    List<UUID> findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(
            @Param("primaryBuildingId") UUID primaryBuildingId,
            @Param("department") String department);

    /**
     * Returns active, non-deleted employee IDs in the reporting subtree rooted at the given manager,
     * constrained to a building and optional department.
     *
     * @param primaryBuildingId the building UUID
     * @param department        the department name, or null to include all departments
     * @param managerId         the root manager's employee UUID
     * @return matching employee IDs including the manager when they belong to the building
     */
    @Query(value = """
            WITH RECURSIVE subtree (id, depth) AS (
                SELECT id, 0 AS depth
                FROM employee
                WHERE id = :managerId AND is_active = true AND deleted_at IS NULL
                UNION ALL
                SELECT e.id, s.depth + 1
                FROM employee e
                JOIN subtree s ON e.manager_id = s.id
                WHERE e.is_active = true AND e.deleted_at IS NULL AND s.depth < 5
            )
            SELECT CAST(e.id AS VARCHAR)
            FROM employee e
            JOIN subtree s ON s.id = e.id
            WHERE e.primary_building_id = :primaryBuildingId
              AND (:department IS NULL OR e.department = :department)
            ORDER BY e.id
            """, nativeQuery = true)
    List<String> findLocationSubtreeEmployeeIds(
            @Param("primaryBuildingId") UUID primaryBuildingId,
            @Param("department") String department,
            @Param("managerId") UUID managerId);

}
