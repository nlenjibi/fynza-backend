package ecommerce.modules.product.service;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.product.dto.AdminProductStatsResponse;
import ecommerce.modules.product.dto.CreateProductRequest;
import ecommerce.modules.product.dto.ProductFilterRequest;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.dto.SellerProductStatsResponse;
import ecommerce.modules.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    Page<ProductResponse> findAll(ProductFilterRequest filter, Pageable pageable);

    ProductResponse findById(UUID id);

    ProductResponse create(CreateProductRequest request, UUID sellerId);

    ProductResponse update(UUID id, UpdateProductRequest request);

    void delete(UUID id);

    Page<ProductResponse> findBySellerId(UUID sellerId, Pageable pageable);

    Page<ProductResponse> findBySellerId(UUID sellerId, ProductStatus status, UUID categoryId, String search, Pageable pageable);

    SellerProductStatsResponse getSellerProductStats(UUID sellerId);

    AdminProductStatsResponse getAdminProductStats();

    ProductResponse addStock(UUID id, int quantity);

    ProductResponse restoreStock(UUID id, int quantity);

    ProductResponse reserveStock(UUID id, int quantity);

    ProductResponse releaseReservedStock(UUID id, int quantity);

    ProductResponse reduceStock(UUID id, int quantity);

    Boolean incrementViewCount(UUID id);

    ProductResponse updateRating(UUID id, Float rating);

    ProductResponse approveProduct(UUID id);

    ProductResponse rejectProduct(UUID id, String reason);

    ProductResponse updateProductStatus(UUID id, ProductStatus status);

    boolean canUpdate(String productId, String userId);

    boolean canDelete(String productId, String userId);
}
