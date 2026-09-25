package ecommerce.modules.settings.repository;

import ecommerce.modules.settings.entity.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for SiteSettings entity.
 * Provides CRUD operations for platform settings management.
 */
@Repository
public interface SiteSettingsRepository extends JpaRepository<SiteSettings, UUID> {
}
