package ecommerce.modules.authz.repository;

import ecommerce.modules.authz.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRole_Id(UUID roleId);

    Optional<RolePermission> findByRole_IdAndPermission_Id(UUID roleId, UUID permissionId);

    boolean existsByRole_IdAndPermission_Id(UUID roleId, UUID permissionId);

    @Query("SELECT rp.permission.code FROM RolePermission rp WHERE rp.role.id = :roleId")
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);
}
