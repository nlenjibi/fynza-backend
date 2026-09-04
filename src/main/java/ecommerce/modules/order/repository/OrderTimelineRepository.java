package ecommerce.modules.order.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.order.entity.OrderTimeline;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderTimelineRepository extends BaseRepository<OrderTimeline, UUID> {

    List<OrderTimeline> findByOrderIdOrderByTimestampDesc(UUID orderId);
}
