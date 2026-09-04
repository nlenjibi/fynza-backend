package ecommerce.modules.product.mapper;

import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.entity.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    ProductResponse toResponse(Product product);


    default List<String> mapImagesToUrls(List<ecommerce.modules.product.entity.ProductImage> images) {
        if (images == null) return null;
        return images.stream().map(ecommerce.modules.product.entity.ProductImage::getImageUrl).toList();
    }

}
