package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.CustomerDetailView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerDetailViewRepository extends JpaRepository<CustomerDetailView, Long> {

    Optional<CustomerDetailView> findByUserId(UUID userId);

    Optional<CustomerDetailView> findByPublicId(UUID publicId);
}
