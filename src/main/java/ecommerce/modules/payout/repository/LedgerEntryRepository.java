package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByFinancialAccountIdOrderByCreatedAtDesc(Long financialAccountId, Pageable pageable);

    Optional<LedgerEntry> findByPublicId(UUID publicId);
}
