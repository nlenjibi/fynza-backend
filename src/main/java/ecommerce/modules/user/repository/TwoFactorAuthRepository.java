package ecommerce.modules.user.repository;

import ecommerce.modules.user.entity.TwoFactorAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, UUID> {

    Optional<TwoFactorAuth> findByUserId(UUID userId);

    @Query("SELECT t FROM TwoFactorAuth t WHERE t.userId = :userId AND t.isEnabled = true")
    Optional<TwoFactorAuth> findEnabledByUserId(@Param("userId") UUID userId);
}
