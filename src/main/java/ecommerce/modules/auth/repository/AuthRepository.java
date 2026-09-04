package ecommerce.modules.auth.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.auth.entity.Auth;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthRepository extends BaseRepository<Auth, UUID> {

        Optional<Auth> findByRefreshToken(String refreshToken);

        @Query("SELECT a FROM Auth a WHERE a.user.id = :userId AND a.isActive = true ORDER BY a.lastActivityAt DESC LIMIT 1")
        Optional<Auth> findTopByUserIdAndIsActiveTrueOrderByLastActivityAtDesc(@Param("userId") UUID userId);

        @Modifying
        @Query("UPDATE Auth a SET a.isActive = false, a.loggedOutAt = :logoutTime " +
                        "WHERE a.user.id = :userId AND a.isActive = true")
        int invalidateAllUserSessions(@Param("userId") UUID userId,
                        @Param("logoutTime") LocalDateTime logoutTime);

        @Modifying
        @Query("UPDATE Auth a SET a.isActive = false " +
                        "WHERE a.isActive = true AND a.expiresAt <= :now")
        int invalidateExpiredSessions(@Param("now") LocalDateTime now);
}
