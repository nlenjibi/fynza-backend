package ecommerce.modules.user.repository;

import ecommerce.modules.user.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    List<PasswordHistory> findByUserIdOrderByChangedAtDesc(UUID userId);

    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.userId = :userId AND ph.isCurrent = true")
    PasswordHistory findCurrentPassword(@Param("userId") UUID userId);
}
