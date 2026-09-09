package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.CustomerStatusHistory;
import ecommerce.modules.customer.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CustomerStatusHistoryRepository extends JpaRepository<CustomerStatusHistory, Long> {

    List<CustomerStatusHistory> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT h FROM CustomerStatusHistory h WHERE h.newStatus = :status AND h.expiresAt IS NOT NULL AND h.expiresAt < :now")
    List<CustomerStatusHistory> findExpiredSuspensions(@Param("status") CustomerStatus status, @Param("now") Instant now);
}
