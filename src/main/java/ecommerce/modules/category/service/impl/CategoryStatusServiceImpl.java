package ecommerce.modules.category.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CategoryStatusRequest;
import ecommerce.modules.category.dto.response.CategoryResponse;
import ecommerce.modules.category.dto.response.CategoryStatusHistoryResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.entity.CategoryStatusHistory;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.repository.CategoryStatusHistoryRepository;
import ecommerce.modules.category.service.CategoryStatusService;
import ecommerce.modules.category.validation.CategoryStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryStatusServiceImpl implements CategoryStatusService {

    private static final Map<String, String> AUDIT_ACTIONS = Map.of(
            "ACTIVE",   AuditAction.CATEGORY_ACTIVATED,
            "INACTIVE", AuditAction.CATEGORY_DEACTIVATED,
            "ARCHIVED", AuditAction.CATEGORY_ARCHIVED,
            "DELETED",  AuditAction.CATEGORY_DELETED
    );

    private final CategoryRepository              categoryRepository;
    private final CategoryStatusHistoryRepository historyRepository;
    private final CategoryStatusTransitionValidator transitionValidator;
    private final CategoryMapper                  mapper;
    private final AuditLogService                 auditLogService;

    @Override
    @Transactional
    public CategoryResponse changeStatus(UUID categoryPublicId, CategoryStatusRequest request, UUID actorUserId) {
        Category category = categoryRepository.findByPublicId(categoryPublicId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryPublicId));

        transitionValidator.validate(category.getStatus(), request.getStatus());

        CategoryStatusHistory history = CategoryStatusHistory.builder()
                .categoryId(category.getId())
                .previousStatus(category.getStatus())
                .newStatus(request.getStatus())
                .reason(request.getReason())
                .changedBy(actorUserId)
                .build();
        historyRepository.save(history);

        category.setStatus(request.getStatus());
        category.setIsActive(request.getStatus() == ecommerce.modules.category.enums.CategoryStatus.ACTIVE);
        categoryRepository.save(category);

        String auditAction = AUDIT_ACTIONS.getOrDefault(request.getStatus().name(), AuditAction.CATEGORY_UPDATED);
        auditLogService.log(AuditLogEntry.builder()
                .action(auditAction)
                .entityType("CATEGORY")
                .entityPublicId(category.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category status changed: publicId={}, {} -> {}", categoryPublicId, history.getPreviousStatus(), request.getStatus());
        return mapper.toResponse(category);
    }

    @Override
    public List<CategoryStatusHistoryResponse> getStatusHistory(UUID categoryPublicId) {
        Category category = categoryRepository.findByPublicId(categoryPublicId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryPublicId));
        return historyRepository.findByCategoryIdOrderByCreatedAtDesc(category.getId())
                .stream().map(mapper::toHistoryResponse).toList();
    }
}
