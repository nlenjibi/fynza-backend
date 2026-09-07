package ecommerce.modules.order.repository;

import ecommerce.modules.order.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {

    List<OrderTimeline> findByOrderIdOrderByTimestampDesc(Long orderId);
}
