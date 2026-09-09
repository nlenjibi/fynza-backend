package ecommerce.modules.authz.repository;

import ecommerce.common.enums.ScopeType;
import ecommerce.modules.authz.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleEntityRepository extends JpaRepository<UserRoleEntity, UUID> {

    List<UserRoleEntity> findByUserIdAndActiveTrue(UUID userId);

    List<UserRoleEntity> findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(UUID userId, ScopeType scopeType, UUID scopeId);

    Optional<UserRoleEntity> findByUserIdAndRole_CodeAndScopeTypeAndActiveTrue(UUID userId, String roleCode, ScopeType scopeType);

    @Query("SELECT rp.permission.code FROM UserRoleEntity ur " +
           "JOIN RolePermission rp ON rp.role.id = ur.role.id " +
           "WHERE ur.userId = :userId AND ur.active = true " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT ur FROM UserRoleEntity ur " +
           "WHERE ur.active = true AND ur.expiresAt IS NOT NULL AND ur.expiresAt < :now")
    List<UserRoleEntity> findExpiredActiveAssignments(@Param("now") Instant now);
}
