package ecommerce.modules.faq.service;

import ecommerce.common.enums.FAQCategory;
import ecommerce.modules.faq.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface FAQService {
    
    Page<FAQResponse> getAllFAQs(Pageable pageable);
    
    Page<FAQResponse> getActiveFAQs(Pageable pageable);
    
    Page<FAQResponse> getFAQsByCategory(FAQCategory category, Pageable pageable);
    
    Page<FAQResponse> searchFAQs(String query, Pageable pageable);
    
    FAQResponse getFAQById(UUID id);
    
    FAQResponse createFAQ(CreateFAQRequest request);
    
    FAQResponse updateFAQ(UUID id, UpdateFAQRequest request);
    
    void deleteFAQ(UUID id);
    
    FAQResponse toggleFAQStatus(UUID id);
    
    FAQStatsResponse getStats();
    
    List<FAQResponse> getPublicFAQs();
    
    List<HelpCategoryResponse> getHelpCategories();
    
    ContactOptionsResponse getContactOptions();
    
    FAQResponse incrementViewCount(UUID id);
}
