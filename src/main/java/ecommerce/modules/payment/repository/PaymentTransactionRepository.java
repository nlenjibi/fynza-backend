package ecommerce.modules.payment.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.payment.entity.PaymentTransaction;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
}
