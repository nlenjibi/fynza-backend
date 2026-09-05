package ecommerce.modules.tag.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.activity.entity.TagActivity;
import ecommerce.modules.activity.repository.TagActivityRepository;
import ecommerce.modules.tag.entity.SellerProductTag;
import ecommerce.modules.tag.entity.Tag;
import ecommerce.modules.tag.repository.SellerProductTagRepository;
import ecommerce.modules.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerProductTagService {

    private final SellerProductTagRepository sellerProductTagRepository;
    private final TagRepository tagRepository;
    private final TagActivityRepository tagActivityRepository;

    @Transactional
    public void assignTagToProduct(UUID sellerId, UUID productId, UUID tagId, UUID userId, String ipAddress) {
        if (sellerProductTagRepository.findByProductIdAndTagId(productId, tagId).isPresent()) {
            throw new BadRequestException("Tag already assigned to this product");
        }

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BadRequestException("Tag not found"));

        SellerProductTag sellerProductTag = SellerProductTag.builder()
                .sellerId(sellerId)
                .productId(productId)
                .tagId(tagId)
                .build();
        sellerProductTagRepository.save(sellerProductTag);

        logActivity(userId, tagId, productId, TagActivity.ActivityType.TAG_ASSIGNED,
                tag.getName(), "Tag assigned to product", ipAddress);

        log.debug("Tag {} assigned to product {} by seller {}", tagId, productId, sellerId);
    }

    @Transactional
    public void unassignTagFromProduct(UUID sellerId, UUID productId, UUID tagId, UUID userId, String ipAddress) {
        SellerProductTag sellerProductTag = sellerProductTagRepository.findByProductIdAndTagId(productId, tagId)
                .orElseThrow(() -> new BadRequestException("Tag not assigned to this product"));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BadRequestException("Tag not found"));

        sellerProductTagRepository.delete(sellerProductTag);

        logActivity(userId, tagId, productId, TagActivity.ActivityType.TAG_UNASSIGNED,
                tag.getName(), "Tag unassigned from product", ipAddress);

        log.debug("Tag {} unassigned from product {} by seller {}", tagId, productId, sellerId);
    }

    @Transactional
    public void setProductTags(UUID sellerId, UUID productId, List<UUID> tagIds, UUID userId, String ipAddress) {
        List<SellerProductTag> currentTags = sellerProductTagRepository.findBySellerIdAndProductId(sellerId, productId);
        List<UUID> currentTagIds = currentTags.stream().map(SellerProductTag::getTagId).toList();

        List<UUID> toRemove = new ArrayList<>(currentTagIds);
        toRemove.removeAll(tagIds);

        for (UUID tagId : toRemove) {
            unassignTagFromProduct(sellerId, productId, tagId, userId, ipAddress);
        }

        for (UUID tagId : tagIds) {
            if (!currentTagIds.contains(tagId)) {
                assignTagToProduct(sellerId, productId, tagId, userId, ipAddress);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Tag> getAvailableTags(UUID sellerId) {
        return tagRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Tag> getTagsForProduct(UUID productId) {
        List<SellerProductTag> productTags = sellerProductTagRepository.findByProductId(productId);
        List<UUID> tagIds = productTags.stream().map(SellerProductTag::getTagId).toList();
        return tagRepository.findAllById(tagIds);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSellerTagStats(UUID sellerId) {
        return sellerProductTagRepository.countByTagGroupedBySeller(sellerId);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(UUID userId, UUID tagId, UUID productId, TagActivity.ActivityType type,
                            String tagName, String description, String ipAddress) {
        TagActivity activity = TagActivity.builder()
                .userId(userId)
                .tagId(tagId)
                .productId(productId)
                .activityType(type)
                .tagName(tagName)
                .description(description)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();
        tagActivityRepository.save(activity);
    }
}
