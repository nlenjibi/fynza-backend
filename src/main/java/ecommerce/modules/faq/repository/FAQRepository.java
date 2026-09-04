package ecommerce.modules.faq.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.common.enums.FAQCategory;
import ecommerce.modules.faq.entity.FAQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FAQRepository extends BaseRepository<FAQ, UUID> {
    
    Page<FAQ> findByIsActiveTrue(Pageable pageable);
    
    Page<FAQ> findByCategoryAndIsActiveTrue(FAQCategory category, Pageable pageable);
    
    Page<FAQ> findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(
            String question, String answer, Pageable pageable);
    
    Page<FAQ> findByIsActiveTrueAndQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(
            Boolean isActive, String question, String answer, Pageable pageable);
    
    Page<FAQ> findByCategory(FAQCategory category, Pageable pageable);
    
    List<FAQ> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    long countByIsActiveTrue();
    
    long countByIsActiveFalse();
}
