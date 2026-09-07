package ecommerce.graphql.resolver.tag;

import ecommerce.modules.tag.dto.TagResponse;
import ecommerce.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller @RequiredArgsConstructor @Slf4j
public class TagResolver {

    private final TagService tagService;

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

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<TagResponse> sellerTags() {
        log.info("GQL sellerTags");
        return tagService.getActiveTags();
    }
}
