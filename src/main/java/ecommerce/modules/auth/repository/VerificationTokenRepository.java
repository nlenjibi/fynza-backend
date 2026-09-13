package ecommerce.modules.auth.repository;

import ecommerce.modules.auth.entity.VerificationToken;
import ecommerce.modules.auth.entity.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByTokenAndTokenType(String token, VerificationTokenType type);

    @Modifying
    @Query("UPDATE VerificationToken v SET v.isUsed = true, v.usedAt = CURRENT_TIMESTAMP " +
           "WHERE v.userId = :userId AND v.tokenType = :type AND v.isUsed = false")
    int invalidateByUserIdAndType(@Param("userId") UUID userId, @Param("type") VerificationTokenType type);
}
