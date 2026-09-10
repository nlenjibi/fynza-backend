package ecommerce.modules.product.service.impl;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.enums.ProductType;
import ecommerce.common.enums.ProductVisibility;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.product.dto.request.CreateProductRequest;
import ecommerce.modules.product.dto.request.UpdateProductRequest;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.entity.ProductCategory;
import ecommerce.modules.product.entity.ProductStatusHistory;
import ecommerce.modules.product.exception.ProductNotFoundException;
import ecommerce.modules.product.mapper.ProductMapper;
import ecommerce.modules.product.policy.ProductOwnershipPolicy;
import ecommerce.modules.product.repository.ProductCategoryRepository;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.repository.ProductStatusHistoryRepository;
import ecommerce.modules.product.service.ProductNumberService;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.product.service.ProductSlugService;
import ecommerce.modules.product.validation.ProductStatusTransitionValidator;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository              productRepository;
    private final ProductCategoryRepository      categoryRepository;
    private final ProductStatusHistoryRepository statusHistoryRepository;
    private final StoreRepository                storeRepository;
    private final ProductOwnershipPolicy         ownershipPolicy;
    private final StoreOwnershipPolicy           storeOwnershipPolicy;
    private final ProductNumberService           numberService;
    private final ProductSlugService             slugService;
    private final ProductStatusTransitionValidator transitionValidator;
    private final ProductMapper                  mapper;
    private final AuditLogService                auditLogService;

    @Override
    @Transactional
    public ProductResponse createProduct(UUID actorUserId, UUID storePublicId, CreateProductRequest request) {
        Store store = storeOwnershipPolicy.assertOwns(storePublicId, actorUserId);

        String slug          = slugService.generateSlug(request.getName());
        String productNumber = numberService.nextProductNumber();
        ProductType type     = request.getProductType() != null ? request.getProductType() : ProductType.SIMPLE;

        Product product = Product.builder()
                .productNumber(productNumber)
                .storeId(store.getId())
                .sellerId(store.getSellerId())
                .name(request.getName())
                .slug(slug)
                .brand(request.getBrand())
                .sku(request.getSku())
                .description(request.getDescription())
                .productType(type)
                .status(ProductStatus.DRAFT)
                .visibility(ProductVisibility.PRIVATE)
                .isActive(true)
                .build();

        productRepository.save(product);

        if (request.getPrimaryCategoryId() != null) {
            categoryRepository.save(ProductCategory.builder()
                    .productId(product.getId())
                    .categoryId(request.getPrimaryCategoryId())
                    .isPrimary(true)
                    .build());
        }

        recordStatusHistory(product.getId(), null, ProductStatus.DRAFT, "Product created", actorUserId);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.PRODUCT_CREATED)
                .entityType("PRODUCT")
                .entityPublicId(product.getId())
                .actorPublicId(actorUserId)
                .reason("Product created: " + product.getName())
                .build());

        log.info("Product created: {} ({})", product.getProductNumber(), product.getId());
        return mapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID actorUserId, UUID productId, UpdateProductRequest request) {
        Product product = ownershipPolicy.assertOwns(productId, actorUserId);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
            product.setSlug(slugService.ensureUnique(request.getName(), product.getId()));
        }
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getBrand() != null)        product.setBrand(request.getBrand());
        if (request.getSku() != null)          product.setSku(request.getSku());

        productRepository.save(product);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.PRODUCT_UPDATED)
                .entityType("PRODUCT")
                .entityPublicId(productId)
                .actorPublicId(actorUserId)
                .reason("Product updated: " + product.getName())
                .build());

        return mapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse archiveProduct(UUID actorUserId, UUID productId) {
        Product product = ownershipPolicy.assertOwns(productId, actorUserId);
        transitionValidator.validate(product.getStatus(), ProductStatus.ARCHIVED);

        ProductStatus previous = product.getStatus();
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);

        recordStatusHistory(productId, previous, ProductStatus.ARCHIVED, "Archived by seller", actorUserId);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.PRODUCT_DELETED)
                .entityType("PRODUCT")
                .entityPublicId(productId)
                .actorPublicId(actorUserId)
                .reason("Product archived: " + product.getName())
                .build());

        return mapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID actorUserId, UUID productId) {
        Product product = ownershipPolicy.assertOwns(productId, actorUserId);
        product.setIsActive(false);
        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
        log.info("Product soft-deleted: {}", productId);
    }

    @Override
    public ProductResponse findById(UUID id) {
        return productRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public ProductResponse findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(slug));
    }

    @Override
    public Page<ProductResponse> findByStore(UUID storePublicId, Pageable pageable) {
        Store store = storeRepository.findByPublicId(storePublicId)
                .orElseThrow(() -> new StoreNotFoundException(storePublicId));
        return productRepository.findByStoreIdAndIsActiveTrue(store.getId(), pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<ProductResponse> findPublicProducts(Pageable pageable) {
        return productRepository.findByStatusAndVisibilityAndIsActiveTrue(
                        ProductStatus.ACTIVE, ProductVisibility.PUBLIC, pageable)
                .map(mapper::toResponse);
    }

    private void recordStatusHistory(UUID productId, ProductStatus from, ProductStatus to,
                                     String reason, UUID changedBy) {
        statusHistoryRepository.save(ProductStatusHistory.builder()
                .productId(productId)
                .previousStatus(from)
                .newStatus(to)
                .reason(reason)
                .changedBy(changedBy)
                .build());
    }
}
