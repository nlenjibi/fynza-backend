package ecommerce.graphql.resolver.tag;

import ecommerce.graphql.input.TagCreateInput;
import ecommerce.graphql.input.TagUpdateInput;
import ecommerce.modules.tag.dto.CreateTagRequest;
import ecommerce.modules.tag.dto.TagResponse;
import ecommerce.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TagResolver {

    private final TagService tagService;

    // =========================================================================
    // QUERIES
    // =========================================================================

    @QueryMapping
    public List<TagResponse> allTags() {
        log.info("GQL allTags");
        return tagService.getAllTags();
    }

    @QueryMapping
    public List<TagResponse> activeTags() {
        log.info("GQL activeTags");
        return tagService.getActiveTags();
    }

    @QueryMapping
    public List<TagResponse> featuredTags() {
        log.info("GQL featuredTags");
        return tagService.getFeaturedTags();
    }

    @QueryMapping
    public List<TagResponse> popularTags(@Argument int limit) {
        log.info("GQL popularTags(limit={})", limit);
        return tagService.getPopularTags(limit);
    }

    @QueryMapping
    public TagResponse tag(@Argument UUID id) {
        log.info("GQL tag(id={})", id);
        return tagService.getTag(id);
    }

    // =========================================================================
    // MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse createTag(@Argument TagCreateInput input) {
        log.info("GQL createTag(name={})", input.getName());
        CreateTagRequest request = CreateTagRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .color(input.getColor())
                .icon(input.getIcon())
                .isFeatured(input.getIsFeatured() != null ? input.getIsFeatured() : false)
                .build();
        return tagService.createTag(request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse updateTag(@Argument UUID id, @Argument TagUpdateInput input) {
        log.info("GQL updateTag(id={})", id);
        CreateTagRequest request = CreateTagRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .color(input.getColor())
                .icon(input.getIcon())
                .isFeatured(input.getIsFeatured())
                .build();
        return tagService.updateTag(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteTag(@Argument UUID id) {
        log.info("GQL deleteTag(id={})", id);
        tagService.deleteTag(id);
        return true;
    }
}
