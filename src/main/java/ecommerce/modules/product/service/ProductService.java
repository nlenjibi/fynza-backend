package ecommerce.modules.product.service;

import ecommerce.modules.product.dto.request.CreateProductRequest;
import ecommerce.modules.product.dto.request.UpdateProductRequest;
import ecommerce.modules.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(UUID actorUserId, UUID storePublicId, CreateProductRequest request);

    ProductResponse updateProduct(UUID actorUserId, UUID productId, UpdateProductRequest request);

    ProductResponse archiveProduct(UUID actorUserId, UUID productId);

    void deleteProduct(UUID actorUserId, UUID productId);

    ProductResponse findById(UUID id);

    ProductResponse findBySlug(String slug);

    Page<ProductResponse> findByStore(UUID storePublicId, Pageable pageable);

    Page<ProductResponse> findPublicProducts(Pageable pageable);
}
