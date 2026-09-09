package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.CustomerSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerSummaryViewRepository
        extends JpaRepository<CustomerSummaryView, Long>, JpaSpecificationExecutor<CustomerSummaryView> {
}
