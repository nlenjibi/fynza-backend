package ecommerce.modules.category.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CategorySuggestionRequest;
import ecommerce.modules.category.dto.response.CategorySuggestionResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.entity.CategorySuggestion;
import ecommerce.modules.category.enums.CategorySuggestionStatus;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.repository.CategorySuggestionRepository;
import ecommerce.modules.category.service.CategorySuggestionService;
import ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategorySuggestionServiceImpl implements CategorySuggestionService {

    private final CategorySuggestionRepository suggestionRepository;
    private final CategoryRepository           categoryRepository;
    private final CategoryMapper               mapper;
    private final AuditLogService              auditLogService;

    @Override
    @Transactional
    public CategorySuggestionResponse submitSuggestion(CategorySuggestionRequest request, UUID requestedBy) {
        Long parentCategoryId = null;
        if (request.getParentCategoryPublicId() != null) {
            Category parent = categoryRepository.findByPublicId(request.getParentCategoryPublicId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getParentCategoryPublicId()));
            parentCategoryId = parent.getId();
        }

        CategorySuggestion suggestion = CategorySuggestion.builder()
                .requestedBy(requestedBy)
                .name(request.getName())
                .description(request.getDescription())
                .parentCategoryId(parentCategoryId)
                .reason(request.getReason())
                .build();

        CategorySuggestion saved = suggestionRepository.save(suggestion);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_SUGGESTION_CREATED)
                .entityType("CATEGORY_SUGGESTION")
                .entityPublicId(saved.getPublicId())
                .actorPublicId(requestedBy)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category suggestion submitted: name={}, requestedBy={}", saved.getName(), requestedBy);
        return mapper.toSuggestionResponse(saved);
    }

    @Override
    public Page<CategorySuggestionResponse> getSuggestions(CategorySuggestionStatus status, Pageable pageable) {
        Page<CategorySuggestion> page = status != null
                ? suggestionRepository.findByStatus(status, pageable)
                : suggestionRepository.findAll(pageable);
        return page.map(mapper::toSuggestionResponse);
    }

    @Override
    public Page<CategorySuggestionResponse> getMySuggestions(UUID requestedBy, Pageable pageable) {
        return suggestionRepository.findByRequestedBy(requestedBy, pageable)
                .map(mapper::toSuggestionResponse);
    }

    @Override
    @Transactional
    public CategorySuggestionResponse approveSuggestion(UUID suggestionPublicId, UUID reviewedBy) {
        CategorySuggestion suggestion = resolveSuggestion(suggestionPublicId);
        suggestion.setStatus(CategorySuggestionStatus.APPROVED);
        suggestion.setReviewedBy(reviewedBy);
        suggestion.setReviewedAt(Instant.now());
        suggestionRepository.save(suggestion);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_SUGGESTION_APPROVED)
                .entityType("CATEGORY_SUGGESTION")
                .entityPublicId(suggestion.getPublicId())
                .actorPublicId(reviewedBy)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category suggestion approved: publicId={}", suggestionPublicId);
        return mapper.toSuggestionResponse(suggestion);
    }

    @Override
    @Transactional
    public CategorySuggestionResponse rejectSuggestion(UUID suggestionPublicId, String reason, UUID reviewedBy) {
        CategorySuggestion suggestion = resolveSuggestion(suggestionPublicId);
        suggestion.setStatus(CategorySuggestionStatus.REJECTED);
        suggestion.setReviewedBy(reviewedBy);
        suggestion.setReviewedAt(Instant.now());
        if (reason != null) suggestion.setReason(reason);
        suggestionRepository.save(suggestion);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_SUGGESTION_REJECTED)
                .entityType("CATEGORY_SUGGESTION")
                .entityPublicId(suggestion.getPublicId())
                .actorPublicId(reviewedBy)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category suggestion rejected: publicId={}", suggestionPublicId);
        return mapper.toSuggestionResponse(suggestion);
    }

    private CategorySuggestion resolveSuggestion(UUID publicId) {
        return suggestionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Category suggestion not found: " + publicId));
    }
}
