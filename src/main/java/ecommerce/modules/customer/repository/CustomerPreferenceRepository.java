package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.CustomerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, Long> {

    Optional<CustomerPreference> findByCustomer_Id(Long customerId);
}
