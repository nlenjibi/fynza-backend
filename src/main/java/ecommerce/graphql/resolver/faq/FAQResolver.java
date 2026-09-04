package ecommerce.graphql.resolver.faq;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.FAQCategoryCount;
import ecommerce.graphql.dto.FAQConnection;
import ecommerce.graphql.dto.ContactOptions;
import ecommerce.graphql.dto.FAQStats;
import ecommerce.graphql.dto.HelpCategory;
import ecommerce.graphql.input.FAQFilterInput;
import ecommerce.graphql.input.FAQCreateInput;
import ecommerce.graphql.input.FAQUpdateInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.faq.dto.ContactOptionsResponse;
import ecommerce.modules.faq.dto.CreateFAQRequest;
import ecommerce.modules.faq.dto.FAQResponse;
import ecommerce.modules.faq.dto.FAQStatsResponse;
import ecommerce.modules.faq.dto.HelpCategoryResponse;
import ecommerce.modules.faq.dto.UpdateFAQRequest;
import ecommerce.modules.faq.service.FAQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FAQResolver {

    private final FAQService faqService;

    // =========================================================================
    // PUBLIC QUERIES
    // =========================================================================

    @QueryMapping
    public FAQConnection faqs(@Argument FAQFilterInput filter,
                               @Argument PageInput pagination) {
        log.info("GQL faqs");
        Pageable pageable = toPageable(pagination);
        Page<FAQResponse> page;

        if (filter != null && filter.getCategory() != null) {
            page = faqService.getFAQsByCategory(filter.getCategory(), pageable);
        } else if (filter != null && filter.getSearch() != null && !filter.getSearch().isEmpty()) {
            page = faqService.searchFAQs(filter.getSearch(), pageable);
        } else {
            page = faqService.getActiveFAQs(pageable);
        }

        return FAQConnection.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public FAQResponse faq(@Argument UUID id) {
        log.info("GQL faq(id={})", id);
        return faqService.getFAQById(id);
    }

    @QueryMapping
    public List<FAQCategoryCount> faqCategories() {
        log.info("GQL faqCategories");
        return faqService.getPublicFAQs().stream()
                .collect(Collectors.groupingBy(FAQResponse::getCategory, Collectors.counting()))
                .entrySet().stream()
                .map(e -> FAQCategoryCount.builder()
                        .category(e.getKey())
                        .count(e.getValue().intValue())
                        .build())
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<HelpCategory> helpCategories() {
        log.info("GQL helpCategories");
        return faqService.getHelpCategories().stream()
                .map(h -> HelpCategory.builder()
                        .name(h.getName())
                        .slug(h.getSlug())
                        .faqs(h.getFaqs())
                        .build())
                .collect(Collectors.toList());
    }

    @QueryMapping
    public ContactOptions contactOptions() {
        log.info("GQL contactOptions");
        ContactOptionsResponse r = faqService.getContactOptions();
        return ContactOptions.builder()
                .liveChat(r.getLiveChat())
                .emailSupport(r.getEmailSupport())
                .phoneSupport(r.getPhoneSupport())
                .phoneHours(r.getPhoneHours())
                .build();
    }

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FAQConnection adminFaqs(@Argument FAQFilterInput filter,
                                    @Argument PageInput pagination) {
        log.info("GQL adminFaqs");
        Pageable pageable = toPageable(pagination);
        Page<FAQResponse> page = faqService.getAllFAQs(pageable);
        return FAQConnection.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FAQStats faqStats() {
        log.info("GQL faqStats");
        FAQStatsResponse r = faqService.getStats();
        return FAQStats.builder()
                .totalFAQs(r.getTotalFAQs())
                .activeFAQs(r.getActiveFAQs())
                .draftFAQs(r.getDraftFAQs())
                .totalViews(r.getTotalViews())
                .build();
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FAQResponse createFAQ(@Argument FAQCreateInput input) {
        log.info("GQL createFAQ");
        CreateFAQRequest request = CreateFAQRequest.builder()
                .question(input.getQuestion())
                .answer(input.getAnswer())
                .category(input.getCategory())
                .displayOrder(input.getDisplayOrder())
                .build();
        return faqService.createFAQ(request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FAQResponse updateFAQ(@Argument UUID id, @Argument FAQUpdateInput input) {
        log.info("GQL updateFAQ(id={})", id);
        UpdateFAQRequest request = UpdateFAQRequest.builder()
                .question(input.getQuestion())
                .answer(input.getAnswer())
                .category(input.getCategory())
                .displayOrder(input.getDisplayOrder())
                .build();
        return faqService.updateFAQ(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteFAQ(@Argument UUID id) {
        log.info("GQL deleteFAQ(id={})", id);
        faqService.deleteFAQ(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FAQResponse toggleFAQActive(@Argument UUID id) {
        log.info("GQL toggleFAQActive(id={})", id);
        return faqService.toggleFAQStatus(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean reorderFAQs(@Argument List<UUID> faqIds) {
        log.info("GQL reorderFAQs");
        return true;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "displayOrder"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
