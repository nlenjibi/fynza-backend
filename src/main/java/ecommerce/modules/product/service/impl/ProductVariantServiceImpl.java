package ecommerce.modules.product.service.impl;

import ecommerce.modules.product.dto.request.CreateVariantRequest;
import ecommerce.modules.product.dto.request.UpdateVariantRequest;
import ecommerce.modules.product.dto.response.ProductVariantResponse;
import ecommerce.modules.product.entity.ProductVariant;
import ecommerce.modules.product.exception.ProductNotFoundException;
import ecommerce.modules.product.mapper.ProductMapper;
import ecommerce.modules.product.policy.ProductOwnershipPolicy;
import ecommerce.modules.product.repository.ProductVariantRepository;
import ecommerce.modules.product.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductOwnershipPolicy   ownershipPolicy;
    private final ProductMapper            mapper;

    @Override
    @Transactional
    public ProductVariantResponse createVariant(UUID actorUserId, UUID productId, CreateVariantRequest request) {
        ownershipPolicy.assertOwns(productId, actorUserId);

        if (variantRepository.existsByProductIdAndSku(productId, request.getSku())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A variant with SKU '" + request.getSku() + "' already exists for this product");
        }

        ProductVariant variant = ProductVariant.builder()
                .productId(productId)
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .variantName(request.getVariantName())
                .size(request.getSize())
                .color(request.getColor())
                .variantStatus("ACTIVE")
                .isActive(true)
                .build();

        variantRepository.save(variant);
        return mapper.toVariantResponse(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(UUID actorUserId, UUID productId, UUID variantId,
                                                UpdateVariantRequest request) {
        ownershipPolicy.assertOwns(productId, actorUserId);

        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductNotFoundException("Variant not found: " + variantId));

        if (request.getSku() != null && !request.getSku().equals(variant.getSku())) {
            if (variantRepository.existsByProductIdAndSku(productId, request.getSku())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "SKU '" + request.getSku() + "' already in use");
            }
            variant.setSku(request.getSku());
        }
        if (request.getBarcode() != null)     variant.setBarcode(request.getBarcode());
        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getSize() != null)        variant.setSize(request.getSize());
        if (request.getColor() != null)       variant.setColor(request.getColor());

        variantRepository.save(variant);
        return mapper.toVariantResponse(variant);
    }

    @Override
    @Transactional
    public void deleteVariant(UUID actorUserId, UUID productId, UUID variantId) {
        ownershipPolicy.assertOwns(productId, actorUserId);

        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductNotFoundException("Variant not found: " + variantId));

        variant.setIsActive(false);
        variantRepository.save(variant);
    }

    @Override
    public List<ProductVariantResponse> getVariants(UUID productId) {
        return variantRepository.findByProductIdAndIsActiveTrueOrderBySku(productId)
                .stream()
                .map(mapper::toVariantResponse)
                .toList();
    }
}
