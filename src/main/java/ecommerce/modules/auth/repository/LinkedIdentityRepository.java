package ecommerce.modules.auth.repository;

import ecommerce.modules.auth.entity.LinkedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkedIdentityRepository extends JpaRepository<LinkedIdentity, UUID> {

    Optional<LinkedIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<LinkedIdentity> findAllByUserId(UUID userId);

    long countByUserId(UUID userId);

    void deleteByUserIdAndProvider(UUID userId, String provider);

    boolean existsByUserIdAndProvider(UUID userId, String provider);
}
