package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnExchangeRepository extends JpaRepository<ReturnExchange, Long> {

    Optional<ReturnExchange> findByReturnId(UUID returnId);

    boolean existsByReturnId(UUID returnId);
}
