package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.SlackChannelMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlackChannelMappingRepository extends JpaRepository<SlackChannelMapping, Long> {

    Optional<SlackChannelMapping> findBySellerIdIsNullAndActiveTrue();

    Optional<SlackChannelMapping> findBySellerIdAndActiveTrue(UUID sellerId);
}
