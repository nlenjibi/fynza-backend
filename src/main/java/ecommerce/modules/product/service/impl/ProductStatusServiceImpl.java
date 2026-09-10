package ecommerce.modules.product.service.impl;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.entity.ProductStatusHistory;
import ecommerce.modules.product.exception.ProductNotFoundException;
import ecommerce.modules.product.mapper.ProductMapper;
import ecommerce.modules.product.policy.ProductOwnershipPolicy;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.repository.ProductStatusHistoryRepository;
import ecommerce.modules.product.service.ProductStatusService;
import ecommerce.modules.product.validation.ProductStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductStatusServiceImpl implements ProductStatusService {

    private final ProductRepository              productRepository;
    private final ProductStatusHistoryRepository statusHistoryRepository;
    private final ProductOwnershipPolicy         ownershipPolicy;
    private final ProductStatusTransitionValidator transitionValidator;
    private final ProductMapper                  mapper;
    private final AuditLogService                auditLogService;

    @Override
    @Transactional
    public ProductResponse submitForReview(UUID actorUserId, UUID productId) {
        Product product = ownershipPolicy.assertOwns(productId, actorUserId);
        return transition(product, ProductStatus.PENDING_REVIEW, "Submitted for review", actorUserId,
                AuditAction.PRODUCT_UPDATED);
    }

    @Override
    @Transactional
    public ProductResponse approveProduct(UUID adminUserId, UUID productId) {
        Product product = requireProduct(productId);
        return transition(product, ProductStatus.ACTIVE, "Approved by admin", adminUserId,
                AuditAction.PRODUCT_APPROVED);
    }

    @Override
    @Transactional
    public ProductResponse rejectProduct(UUID adminUserId, UUID productId, String reason) {
        Product product = requireProduct(productId);
        return transition(product, ProductStatus.INACTIVE, reason, adminUserId,
                AuditAction.PRODUCT_REJECTED);
    }

    @Override
    @Transactional
    public ProductResponse publishProduct(UUID actorUserId, UUID productId) {
        Product product = ownershipPolicy.assertOwns(productId, actorUserId);
        return transition(product, ProductStatus.ACTIVE, "Published by seller", actorUserId,
                AuditAction.PRODUCT_ACTIVATED);
    }

    @Override
    @Transactional
    public ProductResponse deactivateProduct(UUID actorUserId, UUID productId) {
        Product product = ownershipPolicy.assertOwns(productId, actorUserId);
        return transition(product, ProductStatus.INACTIVE, "Deactivated by seller", actorUserId,
                AuditAction.PRODUCT_DEACTIVATED);
    }

    @Override
    @Transactional
    public ProductResponse suspendProduct(UUID adminUserId, UUID productId, String reason) {
        Product product = requireProduct(productId);
        return transition(product, ProductStatus.SUSPENDED, reason, adminUserId,
                AuditAction.PRODUCT_DEACTIVATED);
    }

    @Override
    @Transactional
    public ProductResponse restoreProduct(UUID adminUserId, UUID productId) {
        Product product = requireProduct(productId);
        return transition(product, ProductStatus.ACTIVE, "Restored by admin", adminUserId,
                AuditAction.PRODUCT_ACTIVATED);
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private ProductResponse transition(Product product, ProductStatus to, String reason,
                                       UUID actorId, String auditAction) {
        transitionValidator.validate(product.getStatus(), to);

        ProductStatus from = product.getStatus();
        product.setStatus(to);
        productRepository.save(product);

        statusHistoryRepository.save(ProductStatusHistory.builder()
                .productId(product.getId())
                .previousStatus(from)
                .newStatus(to)
                .reason(reason)
                .changedBy(actorId)
                .build());

        auditLogService.log(AuditLogEntry.builder()
                .action(auditAction)
                .entityType("PRODUCT")
                .entityPublicId(product.getId())
                .actorPublicId(actorId)
                .reason(reason)
                .build());

        return mapper.toResponse(product);
    }
}
