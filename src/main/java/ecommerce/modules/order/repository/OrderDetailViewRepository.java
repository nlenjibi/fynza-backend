package ecommerce.modules.order.repository;

import ecommerce.modules.order.entity.OrderDetailView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDetailViewRepository extends JpaRepository<OrderDetailView, Long> {

    List<OrderDetailView> findByOrderPublicId(UUID orderPublicId);

    List<OrderDetailView> findByOrderNumber(String orderNumber);
}
