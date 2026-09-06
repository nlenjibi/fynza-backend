package ecommerce.modules.product.service.impl;

import ecommerce.common.enums.InventoryStatus;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.ProductStatus;
import ecommerce.common.enums.Role;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.product.dto.*;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.entity.ProductVariant;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.repository.ProductVariantRepository;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Override
    @Cacheable(value = "products-filter", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#filter=' + #filter.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize).getBytes())")
    public Page<ProductResponse> findAll(ProductFilterRequest filter, Pageable pageable) {
        log.debug("Finding all products with filter: {}", filter);

        Page<Product> products;

        if (filter.getCategoryId() != null) {
            products = productRepository.findByCategory_PublicId(filter.getCategoryId(), pageable);
        } else if (filter.getSellerId() != null) {
            products = productRepository.findBySeller_PublicId(filter.getSellerId(), pageable);
        } else if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
            products = productRepository.findByBrandIgnoreCase(filter.getBrand(), pageable);
        } else if (filter.getBrands() != null && !filter.getBrands().isEmpty()) {
            products = productRepository.findByBrandInIgnoreCase(filter.getBrands(), pageable);
        } else if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            products = productRepository.searchByKeyword(filter.getSearch(), pageable);
        } else if (filter.getStatus() != null) {
            products = productRepository.findByStatus(
                    ecommerce.common.enums.ProductStatus.valueOf(filter.getStatus()),
                    pageable
            );
        } else {
            products = productRepository.findAll(pageable);
        }

        // Batch fetch variants and seller profiles to avoid N+1 queries
        return mapPageToResponse(products);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(UUID id) {
        log.debug("Finding product by id: {}", id);

        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return mapToResponse(product);
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#result.id")
    @CacheEvict(value = {"products-filter", "products-page"}, allEntries = true)
    public ProductResponse create(CreateProductRequest request, UUID sellerId) {
        log.info("Creating new product: {} for seller: {}", request.getName(), sellerId);

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByPublicId(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .category(category)
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .brand(request.getBrand())
                .seller(seller)
                .build();

        if (request.getOriginalPrice() != null && request.getPrice() != null
                && request.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = request.getOriginalPrice()
                    .subtract(request.getPrice())
                    .divide(request.getOriginalPrice(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            product.setDiscount(discount);
        }

        Product savedProduct = productRepository.save(product);

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(variantRequest -> ProductVariant.builder()
                            .product(savedProduct)
                            .sku(variantRequest.getSku())
                            .size(variantRequest.getSize())
                            .color(variantRequest.getColor())
                            .priceOverride(variantRequest.getPrice())
                            .stock(variantRequest.getStock() != null ? variantRequest.getStock() : 0)
                            .build())
                    .collect(Collectors.toList());
            productVariantRepository.saveAll(variants);
        }

        log.info("Product created successfully with id: {}", savedProduct.getId());
        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#id")
    @CacheEvict(value = {"products-filter", "products-page"}, allEntries = true)
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }

        if (request.getOriginalPrice() != null && product.getOriginalPrice() != null
                && product.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0
                && product.getPrice() != null) {
            BigDecimal discount = product.getOriginalPrice()
                    .subtract(product.getPrice())
                    .divide(product.getOriginalPrice(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            product.setDiscount(discount);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByPublicId(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getId());

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "products-filter", "products-page"}, allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting product with id: {}", id);

        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        productRepository.delete(product);
        log.info("Product deleted successfully: {}", id);
    }

    @Override
    public Page<ProductResponse> findBySellerId(UUID sellerId, Pageable pageable) {
        log.info("Finding products for seller: {}", sellerId);
        Page<Product> products = productRepository.findBySeller_PublicId(sellerId, pageable);
        return mapPageToResponse(products);
    }

    /**
     * Maps a page of products to responses using batch-fetched data to avoid N+1 queries.
     * @param products Page of products
     * @return Page of product responses
     */
    private Page<ProductResponse> mapPageToResponse(Page<Product> products) {
        if (products.isEmpty()) {
            return products.map(this::mapToResponse);
        }

        // Collect all product public IDs for batch fetching
        List<UUID> productIds = products.getContent().stream()
                .map(Product::getPublicId)
                .collect(Collectors.toList());

        // Collect unique seller IDs (User.id is UUID)
        List<UUID> sellerIds = products.getContent().stream()
                .map(p -> p.getSeller() != null ? p.getSeller().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // Batch fetch variants for all products (single query)
        Map<UUID, List<ProductVariant>> variantsMap = productVariantRepository.findByProduct_PublicIdIn(productIds)
                .stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getPublicId()));

        // Batch fetch seller profiles for all sellers (single query)
        Map<UUID, SellerProfile> sellerProfileMap = sellerProfileRepository.findByUser_PublicIdIn(sellerIds)
                .stream()
                .collect(Collectors.toMap(sp -> sp.getUser().getId(), sp -> sp));

        // Map with pre-fetched data
        return products.map(product -> mapToResponseWithCache(product, variantsMap, sellerProfileMap));
    }

    /**
     * Map product to response using pre-fetched variant and seller profile data.
     */
    private ProductResponse mapToResponseWithCache(Product product, 
            Map<UUID, List<ProductVariant>> variantsMap,
            Map<UUID, SellerProfile> sellerProfileMap) {

        // Use pre-fetched variants
        List<ProductVariantResponse> variantResponses = new ArrayList<>();
        if (product.getPublicId() != null && variantsMap.containsKey(product.getPublicId())) {
            variantResponses = variantsMap.get(product.getPublicId()).stream()
                    .map(variant -> ProductVariantResponse.builder()
                            .id(variant.getPublicId())
                            .sku(variant.getSku())
                            .size(variant.getSize())
                            .color(variant.getColor())
                            .price(variant.getPriceOverride())
                            .stock(variant.getStock())
                            .build())
                    .collect(Collectors.toList());
        }

        // Use pre-fetched category
        CategoryInfo categoryInfo = null;
        if (product.getCategory() != null) {
            categoryInfo = CategoryInfo.builder()
                    .id(product.getCategory().getPublicId())
                    .name(product.getCategory().getName())
                    .slug(product.getCategory().getSlug())
                    .build();
        }

        // Use pre-fetched seller profile
        SellerInfo sellerInfo = null;
        if (product.getSeller() != null) {
            SellerProfile sellerProfile = sellerProfileMap.get(product.getSeller().getId());

            if (sellerProfile != null) {
                sellerInfo = SellerInfo.builder()
                        .id(sellerProfile.getPublicId())
                        .storeName(sellerProfile.getStoreName())
                        .rating(sellerProfile.getRating())
                        .build();
            } else {
                sellerInfo = SellerInfo.builder()
                        .id(product.getSeller().getId())
                        .storeName(product.getSeller().getFullName())
                        .build();
            }
        }

        return ProductResponse.builder()
                .id(product.getPublicId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .brand(product.getBrand())
                .sku(product.getSku())
                .category(categoryInfo)
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discount(product.getDiscount())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .viewCount(product.getViewCount())
                .images(new ArrayList<>())
                .variants(variantResponses)
                .inStock(product.isInStock())
                .stockCount(product.getStock())
                .availableQuantity(product.getAvailableQuantity())
                .soldQuantity(product.getSoldQuantity())
                .seller(sellerInfo)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .isApproved(product.getIsApproved())
                .inventoryStatus(product.getInventoryStatus() != null ? product.getInventoryStatus().name() : null)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }

    /**
     * Original mapToResponse - kept for single product operations (create, update).
     */
    private ProductResponse mapToResponse(Product product) {
        List<ProductVariantResponse> variantResponses = new ArrayList<>();
        if (product.getId() != null) {
            List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
            variantResponses = variants.stream()
                    .map(variant -> ProductVariantResponse.builder()
                            .id(variant.getPublicId())
                            .sku(variant.getSku())
                            .size(variant.getSize())
                            .color(variant.getColor())
                            .price(variant.getPriceOverride())
                            .stock(variant.getStock())
                            .build())
                    .collect(Collectors.toList());
        }

        CategoryInfo categoryInfo = null;
        if (product.getCategory() != null) {
            categoryInfo = CategoryInfo.builder()
                    .id(product.getCategory().getPublicId())
                    .name(product.getCategory().getName())
                    .slug(product.getCategory().getSlug())
                    .build();
        }

        SellerInfo sellerInfo = null;
        if (product.getSeller() != null) {
            SellerProfile sellerProfile = sellerProfileRepository
                    .findByUser_PublicId(product.getSeller().getId())
                    .orElse(null);

            if (sellerProfile != null) {
                sellerInfo = SellerInfo.builder()
                        .id(sellerProfile.getPublicId())
                        .storeName(sellerProfile.getStoreName())
                        .rating(sellerProfile.getRating())
                        .build();
            } else {
                sellerInfo = SellerInfo.builder()
                        .id(product.getSeller().getId())
                        .storeName(product.getSeller().getFullName())
                        .build();
            }
        }

        return ProductResponse.builder()
                .id(product.getPublicId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .brand(product.getBrand())
                .sku(product.getSku())
                .category(categoryInfo)
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discount(product.getDiscount())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .viewCount(product.getViewCount())
                .images(new ArrayList<>())
                .variants(variantResponses)
                .inStock(product.isInStock())
                .stockCount(product.getStock())
                .availableQuantity(product.getAvailableQuantity())
                .soldQuantity(product.getSoldQuantity())
                .seller(sellerInfo)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .isApproved(product.getIsApproved())
                .inventoryStatus(product.getInventoryStatus() != null ? product.getInventoryStatus().name() : null)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }

    @Override
    @Transactional
    public ProductResponse addStock(UUID id, int quantity) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.addStock(quantity);
        productRepository.save(product);
        log.info("Added {} stock to product {}", quantity, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse restoreStock(UUID id, int quantity) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.releaseReservedStock(quantity);
        productRepository.save(product);
        log.info("Restored {} stock to product {}", quantity, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse releaseReservedStock(UUID id, int quantity) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.releaseReservedStock(quantity);
        productRepository.save(product);
        log.info("Released {} reserved stock for product {}", quantity, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse reserveStock(UUID id, int quantity) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.reserveStock(quantity);
        productRepository.save(product);
        log.info("Reserved {} stock for product {}", quantity, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse reduceStock(UUID id, int quantity) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.reduceStock(quantity);
        productRepository.save(product);
        log.info("Reduced {} stock from product {}", quantity, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public Boolean incrementViewCount(UUID id) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setViewCount((product.getViewCount() != null ? product.getViewCount() : 0) + 1);
        productRepository.save(product);
        log.debug("Incremented view count for product {}", id);
        return true;
    }

    @Override
    @Transactional
    public ProductResponse updateRating(UUID id, Float rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setRating(BigDecimal.valueOf(rating.doubleValue()));
        productRepository.save(product);
        log.info("Updated rating to {} for product {}", rating, id);
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findBySellerId(UUID sellerId, ProductStatus status, UUID categoryId, String search, Pageable pageable) {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .sellerId(sellerId)
                .status(status != null ? status.name() : null)
                .categoryId(categoryId)
                .search(search)
                .keyword(search)
                .build();
        return findAll(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerProductStatsResponse getSellerProductStats(UUID sellerId) {
        long total = productRepository.countBySeller_Id(sellerId);
        long active = productRepository.countBySeller_IdAndStatus(sellerId, ProductStatus.ACTIVE);
        long draft = productRepository.countBySeller_IdAndStatus(sellerId, ProductStatus.DRAFT);
        long outOfStock = productRepository.countBySeller_IdAndInventoryStatus(sellerId, InventoryStatus.OUT_OF_STOCK);
        long lowStock = productRepository.countBySeller_IdAndLowStock(sellerId);

        return SellerProductStatsResponse.builder()
                .totalProducts(total)
                .activeProducts(active)
                .draftProducts(draft)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductStatsResponse getAdminProductStats() {
        long total = productRepository.count();
        long active = productRepository.countByStatus(ProductStatus.ACTIVE);
        long pending = productRepository.countPendingApproval();
        long outOfStock = productRepository.countByInventoryStatusAndIsActiveTrue(InventoryStatus.OUT_OF_STOCK);
        long lowStock = productRepository.countByInventoryStatusAndIsActiveTrue(InventoryStatus.LOW_STOCK);

        return AdminProductStatsResponse.builder()
                .totalProducts(total)
                .activeProducts(active)
                .pendingProducts(pending)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .build();
    }

    @Override
    @Transactional
    public ProductResponse approveProduct(UUID id) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        
        product.setIsApproved(true);
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);
        log.info("Approved product: {}", id);
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse rejectProduct(UUID id, String reason) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        
        product.setIsApproved(false);
        product.setStatus(ProductStatus.DRAFT);
        product.setDescription(product.getDescription() + "\n\n rejection reason: " + reason);
        Product saved = productRepository.save(product);
        log.info("Rejected product: {} with reason: {}", id, reason);
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProductStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        
        product.setStatus(status);
        Product saved = productRepository.save(product);
        log.info("Updated product {} status to {}", id, status);
        
        return mapToResponse(saved);
    }

    @Override
    public boolean canUpdate(String productId, String userId) {
        UUID productUuid = UUID.fromString(productId);
        UUID userUuid = UUID.fromString(userId);
        Product product = productRepository.findByPublicId(productUuid).orElse(null);
        if (product == null) {
            return false;
        }
        boolean isOwner = product.getSeller() != null && product.getSeller().getId().equals(userUuid);
        boolean isAdmin = product.getSeller() != null && product.getSeller().getRole() == Role.ADMIN;
        return isOwner || isAdmin;
    }

    @Override
    public boolean canDelete(String productId, String userId) {
        UUID productUuid = UUID.fromString(productId);
        UUID userUuid = UUID.fromString(userId);
        Product product = productRepository.findByPublicId(productUuid).orElse(null);
        if (product == null) {
            return false;
        }
        boolean isOwner = product.getSeller() != null && product.getSeller().getId().equals(userUuid);
        boolean isAdmin = product.getSeller() != null && product.getSeller().getRole() == Role.ADMIN;
        return isOwner || isAdmin;
    }
}
