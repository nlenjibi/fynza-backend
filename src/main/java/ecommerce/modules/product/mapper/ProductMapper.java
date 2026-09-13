package ecommerce.modules.product.mapper;

import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.dto.response.ProductVariantResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .productNumber(p.getProductNumber())
                .storeId(p.getStoreId())
                .sellerId(p.getSellerId())
                .name(p.getName())
                .slug(p.getSlug())
                .brand(p.getBrand())
                .sku(p.getSku())
                .description(p.getDescription())
                .productType(p.getProductType())
                .status(p.getStatus())
                .visibility(p.getVisibility())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public ProductVariantResponse toVariantResponse(ProductVariant v) {
        return ProductVariantResponse.builder()
                .id(v.getId())
                .productId(v.getProductId())
                .sku(v.getSku())
                .barcode(v.getBarcode())
                .variantName(v.getVariantName())
                .variantStatus(v.getVariantStatus())
                .size(v.getSize())
                .color(v.getColor())
                .isActive(v.getIsActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
