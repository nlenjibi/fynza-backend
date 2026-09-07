package ecommerce.modules.notification.service;

import ecommerce.modules.notification.entity.SlackChannelMapping;
import ecommerce.modules.notification.repository.SlackChannelMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the Slack channel ID for a broadcast notification based on optional seller context.
 * sellerId == null resolves the global/admin-wide channel only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackChannelResolver {

    private final SlackChannelMappingRepository repository;

    public Optional<String> resolve(UUID sellerId) {
        if (sellerId == null) {
            return repository.findBySellerIdIsNullAndActiveTrue()
                    .map(SlackChannelMapping::getSlackChannelId);
        }
        return repository.findBySellerIdAndActiveTrue(sellerId)
                .map(SlackChannelMapping::getSlackChannelId);
    }
}
