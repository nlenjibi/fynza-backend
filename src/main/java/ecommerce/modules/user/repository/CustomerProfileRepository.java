package ecommerce.modules.user.repository;

import ecommerce.modules.user.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByUserId(UUID userId);

    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.user.email = :email")
    Optional<CustomerProfile> findByUserEmail(String email);
}
