package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductStatusHistoryRepository extends JpaRepository<ProductStatusHistory, Long> {

    Page<ProductStatusHistory> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
}
