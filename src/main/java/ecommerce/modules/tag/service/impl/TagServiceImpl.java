package ecommerce.modules.tag.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.tag.dto.CreateTagRequest;
import ecommerce.modules.tag.dto.TagResponse;
import ecommerce.modules.tag.entity.Tag;
import ecommerce.modules.tag.repository.TagRepository;
import ecommerce.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        if (tagRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tag with name '" + request.getName() + "' already exists");
        }

        Tag tag = Tag.builder()
                .name(request.getName().toLowerCase().trim())
                .description(request.getDescription())
                .color(request.getColor())
                .icon(request.getIcon())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .build();

        Tag savedTag = tagRepository.save(tag);
        log.info("Created new tag: {}", savedTag.getName());
        return mapToResponse(savedTag);
    }

    @Override
    public TagResponse getTag(UUID tagId) {
        Tag tag = tagRepository.findByPublicId(tagId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Tag", tagId));
        return mapToResponse(tag);
    }

    @Override
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagResponse> getActiveTags() {
        return tagRepository.findByIsActiveTrue(PageRequest.of(0, 100)).getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagResponse> getFeaturedTags() {
        return tagRepository.findByIsFeaturedTrue(PageRequest.of(0, 20)).getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagResponse> getPopularTags(int limit) {
        return tagRepository.findMostUsedTags(PageRequest.of(0, limit)).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TagResponse updateTag(UUID tagId, CreateTagRequest request) {
        Tag tag = tagRepository.findByPublicId(tagId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Tag", tagId));

        if (!tag.getName().equalsIgnoreCase(request.getName()) && tagRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tag with name '" + request.getName() + "' already exists");
        }

        tag.setName(request.getName().toLowerCase().trim());
        tag.setDescription(request.getDescription());
        tag.setColor(request.getColor());
        tag.setIcon(request.getIcon());
        if (request.getIsFeatured() != null) {
            tag.setIsFeatured(request.getIsFeatured());
        }

        Tag updatedTag = tagRepository.save(tag);
        log.info("Updated tag: {}", updatedTag.getName());
        return mapToResponse(updatedTag);
    }

    @Override
    @Transactional
    public void deleteTag(UUID tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException("Tag not found with id: " + tagId);
        }
        tagRepository.deleteById(tagId);
        log.info("Deleted tag with id: {}", tagId);
    }

    @Override
    @Transactional
    public void incrementUsage(UUID tagId) {
        tagRepository.incrementUsageCount(tagId);
    }

    @Override
    @Transactional
    public void decrementUsage(UUID tagId) {
        tagRepository.decrementUsageCount(tagId);
    }

    @Override
    @Transactional
    public TagResponse getOrCreateTag(String name) {
        return tagRepository.findByName(name.toLowerCase().trim())
                .map(this::mapToResponse)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder()
                            .name(name.toLowerCase().trim())
                            .build();
                    return mapToResponse(tagRepository.save(newTag));
                });
    }

    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .color(tag.getColor())
                .icon(tag.getIcon())
                .isFeatured(tag.getIsFeatured())
                .usageCount(tag.getUsageCount())
                .build();
    }
}
