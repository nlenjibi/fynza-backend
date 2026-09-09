package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByPublicId(UUID publicId);

    Optional<Customer> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
