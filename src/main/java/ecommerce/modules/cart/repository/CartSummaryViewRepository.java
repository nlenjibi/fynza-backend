package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.CartSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartSummaryViewRepository
        extends JpaRepository<CartSummaryView, Long>, JpaSpecificationExecutor<CartSummaryView> {

    Optional<CartSummaryView> findByUserId(UUID userId);

    Optional<CartSummaryView> findByCartToken(String cartToken);
}
