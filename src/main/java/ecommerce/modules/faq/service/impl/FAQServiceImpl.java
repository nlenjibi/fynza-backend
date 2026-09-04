package ecommerce.modules.faq.service;

import ecommerce.common.enums.FAQCategory;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.faq.dto.*;
import ecommerce.modules.faq.entity.FAQ;
import ecommerce.modules.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FAQServiceImpl implements FAQService {

    private final FAQRepository faqRepository;

    @Override
    public Page<FAQResponse> getAllFAQs(Pageable pageable) {
        log.debug("Getting all FAQs with pagination");
        return faqRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Cacheable(value = "faqs", key = "'active_' + #pageable.pageNumber")
    public Page<FAQResponse> getActiveFAQs(Pageable pageable) {
        log.debug("Getting active FAQs");
        return faqRepository.findByIsActiveTrue(pageable).map(this::toResponse);
    }

    @Override
    @Cacheable(value = "faqs", key = "'category_' + #category.name() + '_' + #pageable.pageNumber")
    public Page<FAQResponse> getFAQsByCategory(FAQCategory category, Pageable pageable) {
        log.debug("Getting FAQs by category: {}", category);
        return faqRepository.findByCategoryAndIsActiveTrue(category, pageable).map(this::toResponse);
    }

    @Override
    @Cacheable(value = "faqs", key = "'search_' + #query.hashCode() + '_' + #pageable.pageNumber")
    public Page<FAQResponse> searchFAQs(String query, Pageable pageable) {
        log.debug("Searching FAQs with query: {}", query);
        return faqRepository.findByIsActiveTrueAndQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(
                true, query, query, pageable).map(this::toResponse);
    }

    @Override
    public FAQResponse getFAQById(UUID id) {
        log.debug("Getting FAQ by id: {}", id);
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
        return toResponse(faq);
    }

    @Override
    @Transactional
    @CacheEvict(value = "faqs", allEntries = true)
    public FAQResponse createFAQ(CreateFAQRequest request) {
        log.info("Creating new FAQ with question: {}", request.getQuestion());
        
        FAQ faq = FAQ.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .category(request.getCategory())
                .isActive(true)
                .viewCount(0)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        
        FAQ saved = faqRepository.save(faq);
        log.info("FAQ created with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "faqs", allEntries = true)
    public FAQResponse updateFAQ(UUID id, UpdateFAQRequest request) {
        log.info("Updating FAQ with id: {}", id);
        
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
        
        if (request.getQuestion() != null) {
            faq.setQuestion(request.getQuestion());
        }
        if (request.getAnswer() != null) {
            faq.setAnswer(request.getAnswer());
        }
        if (request.getCategory() != null) {
            faq.setCategory(request.getCategory());
        }
        if (request.getDisplayOrder() != null) {
            faq.setDisplayOrder(request.getDisplayOrder());
        }
        
        FAQ saved = faqRepository.save(faq);
        log.info("FAQ updated with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "faqs", allEntries = true)
    public void deleteFAQ(UUID id) {
        log.info("Deleting FAQ with id: {}", id);
        
        if (!faqRepository.existsById(id)) {
            throw new ResourceNotFoundException("FAQ not found");
        }
        
        faqRepository.deleteById(id);
        log.info("FAQ deleted with id: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "faqs", allEntries = true)
    public FAQResponse toggleFAQStatus(UUID id) {
        log.info("Toggling FAQ status for id: {}", id);
        
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
        
        faq.setIsActive(!faq.getIsActive());
        FAQ saved = faqRepository.save(faq);
        
        log.info("FAQ {} status changed to: {}", id, saved.getIsActive());
        return toResponse(saved);
    }

    @Override
    public FAQStatsResponse getStats() {
        log.debug("Getting FAQ statistics");
        
        long total = faqRepository.count();
        long active = faqRepository.countByIsActiveTrue();
        long draft = faqRepository.countByIsActiveFalse();
        
        long totalViews = faqRepository.findAll().stream()
                .mapToLong(f -> f.getViewCount() != null ? f.getViewCount().longValue() : 0L)
                .sum();
        
        return FAQStatsResponse.builder()
                .totalFAQs(total)
                .activeFAQs(active)
                .draftFAQs(draft)
                .totalViews(totalViews)
                .build();
    }

    @Override
    @Cacheable(value = "faqs", key = "'public_' + 'all'")
    public List<FAQResponse> getPublicFAQs() {
        log.debug("Getting public FAQs");
        return faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "faqs", key = "'help_categories'")
    public List<HelpCategoryResponse> getHelpCategories() {
        log.debug("Getting help categories");
        
        return faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .collect(Collectors.groupingBy(FAQ::getCategory))
                .entrySet().stream()
                .map(entry -> {
                    FAQCategory category = entry.getKey();
                    List<FAQ> faqs = entry.getValue();
                    
                    return HelpCategoryResponse.builder()
                            .name(category.name())
                            .slug(category.name().toLowerCase().replace("_", "-"))
                            .faqs(faqs.stream().map(this::toResponse).collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "faqs", key = "'contact_options'")
    public ContactOptionsResponse getContactOptions() {
        log.debug("Getting contact options");
        
        // Return default contact options - these could be configurable in the future
        return ContactOptionsResponse.builder()
                .liveChat("https://chat.example.com")
                .emailSupport("support@example.com")
                .phoneSupport("+1-800-123-4567")
                .phoneHours("Mon-Fri 9AM-6PM EST")
                .build();
    }

    @Override
    @Transactional
    public FAQResponse incrementViewCount(UUID id) {
        log.debug("Incrementing view count for FAQ: {}", id);
        
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
        
        faq.setViewCount((faq.getViewCount() != null ? faq.getViewCount() : 0) + 1);
        FAQ saved = faqRepository.save(faq);
        
        return toResponse(saved);
    }

    private FAQResponse toResponse(FAQ faq) {
        return FAQResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .category(faq.getCategory())
                .isActive(faq.getIsActive())
                .viewCount(faq.getViewCount())
                .displayOrder(faq.getDisplayOrder())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
