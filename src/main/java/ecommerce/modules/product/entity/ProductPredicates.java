package ecommerce.modules.product.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductPredicates {

    private final BooleanBuilder builder;

    public ProductPredicates() {
        this.builder = new BooleanBuilder();
    }

    public static ProductPredicates builder() {
        return new ProductPredicates();
    }

    public ProductPredicates withStatus(ecommerce.common.enums.ProductStatus status) {
        if (status != null) {
            builder.and(QProduct.product.status.eq(status));
        }
        return this;
    }

    public ProductPredicates withSearchTerm(String searchTerm) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            String term = searchTerm.toLowerCase();
            builder.and(QProduct.product.name.lower().contains(term)
                    .or(QProduct.product.description.lower().contains(term)));
        }
        return this;
    }

    public ProductPredicates withCategoryId(UUID categoryId) {
        if (categoryId != null) {
            builder.and(QProduct.product.category.id.eq(categoryId));
        }
        return this;
    }

    public ProductPredicates withCategoryIds(List<UUID> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            builder.and(QProduct.product.category.id.in(categoryIds));
        }
        return this;
    }

    public ProductPredicates withBrandId(UUID brandId) {
        if (brandId != null) {
            builder.and(QProduct.product.seller.id.eq(brandId));
        }
        return this;
    }

    public ProductPredicates withBrand(String brand) {
        if (brand != null && !brand.isBlank()) {
            builder.and(QProduct.product.brand.equalsIgnoreCase(brand));
        }
        return this;
    }

    public ProductPredicates withBrands(List<String> brands) {
        if (brands != null && !brands.isEmpty()) {
            builder.and(QProduct.product.brand.lower().in(brands.stream()
                    .map(String::toLowerCase)
                    .toList()));
        }
        return this;
    }

    public ProductPredicates withMinPrice(BigDecimal minPrice) {
        if (minPrice != null) {
            builder.and(QProduct.product.price.goe(minPrice));
        }
        return this;
    }

    public ProductPredicates withMaxPrice(BigDecimal maxPrice) {
        if (maxPrice != null) {
            builder.and(QProduct.product.price.loe(maxPrice));
        }
        return this;
    }

    public ProductPredicates withMinRating(BigDecimal minRating) {
        if (minRating != null) {
            builder.and(QProduct.product.rating.goe(minRating));
        }
        return this;
    }

    public ProductPredicates withMaxRating(BigDecimal maxRating) {
        if (maxRating != null) {
            builder.and(QProduct.product.rating.loe(maxRating));
        }
        return this;
    }

    public ProductPredicates withInStock(Boolean inStock) {
        if (Boolean.TRUE.equals(inStock)) {
            builder.and(QProduct.product.stock.gt(0)
                    .or(QProduct.product.availableQuantity.gt(0)));
        }
        return this;
    }

    public ProductPredicates withMinDiscount(BigDecimal minDiscount) {
        if (minDiscount != null) {
            builder.and(QProduct.product.discount.goe(minDiscount));
        }
        return this;
    }

    public ProductPredicates withMaxDiscount(BigDecimal maxDiscount) {
        if (maxDiscount != null) {
            builder.and(QProduct.product.discount.loe(maxDiscount));
        }
        return this;
    }

    public ProductPredicates withFeatured(Boolean featured) {
        if (featured != null) {
            builder.and(QProduct.product.featured.eq(featured));
        }
        return this;
    }

    public ProductPredicates withIsNew(Boolean isNew) {
        if (isNew != null) {
            builder.and(QProduct.product.isNew.eq(isNew));
        }
        return this;
    }

    public ProductPredicates withBestseller(Boolean bestseller) {
        if (bestseller != null) {
            builder.and(QProduct.product.isBestseller.eq(bestseller));
        }
        return this;
    }

    public ProductPredicates withMinViews(Long minViews) {
        if (minViews != null) {
            builder.and(QProduct.product.viewCount.goe(minViews));
        }
        return this;
    }

    public ProductPredicates withMinSales(Long minSales) {
        if (minSales != null) {
            builder.and(QProduct.product.soldQuantity.goe(minSales));
        }
        return this;
    }

    public Predicate build() {
        return builder.getValue();
    }
}
