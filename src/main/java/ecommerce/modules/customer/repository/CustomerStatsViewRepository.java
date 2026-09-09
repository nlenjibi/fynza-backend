package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.CustomerStatsView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerStatsViewRepository extends JpaRepository<CustomerStatsView, Integer> {
}
