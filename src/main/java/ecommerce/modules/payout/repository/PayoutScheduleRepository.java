package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.PayoutScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutScheduleRepository extends JpaRepository<PayoutScheduleEntity, Long> {

    Optional<PayoutScheduleEntity> findBySellerId(Long sellerId);

    Optional<PayoutScheduleEntity> findByPublicId(UUID publicId);

    @Query("SELECT s FROM PayoutScheduleEntity s WHERE s.enabled = true AND s.nextRunAt <= :now")
    List<PayoutScheduleEntity> findDueSchedules(@Param("now") Instant now);
}
