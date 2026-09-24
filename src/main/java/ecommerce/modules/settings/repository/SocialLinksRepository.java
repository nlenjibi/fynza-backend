package ecommerce.modules.settings.repository;

import ecommerce.modules.settings.entity.SocialLinks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for SocialLinks entity.
 * Provides CRUD operations for social media links management.
 */
@Repository
public interface SocialLinksRepository extends JpaRepository<SocialLinks, UUID> {
}
