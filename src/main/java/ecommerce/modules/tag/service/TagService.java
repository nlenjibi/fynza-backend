package ecommerce.modules.tag.service;

import ecommerce.modules.tag.dto.CreateTagRequest;
import ecommerce.modules.tag.dto.TagResponse;

import java.util.List;
import java.util.UUID;

public interface TagService {

    TagResponse createTag(CreateTagRequest request);

    TagResponse getTag(UUID tagId);

    List<TagResponse> getAllTags();

    List<TagResponse> getActiveTags();

    List<TagResponse> getFeaturedTags();

    List<TagResponse> getPopularTags(int limit);

    TagResponse updateTag(UUID tagId, CreateTagRequest request);

    void deleteTag(UUID tagId);

    void incrementUsage(UUID tagId);

    void decrementUsage(UUID tagId);

    TagResponse getOrCreateTag(String name);
}
