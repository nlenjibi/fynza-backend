package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.StoreStatusHistory;
import ecommerce.modules.store.enums.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StoreStatusHistoryRepository extends JpaRepository<StoreStatusHistory, Long> {

    List<StoreStatusHistory> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT h FROM StoreStatusHistory h WHERE h.newStatus = :status AND h.expiresAt IS NOT NULL AND h.expiresAt <= :now")
    List<StoreStatusHistory> findExpiredSuspensions(StoreStatus status, Instant now);
}
