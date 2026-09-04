package ecommerce.modules.cart.mapper;

import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.entity.CartItem;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.mapper.ProductMapper;
import org.mapstruct.Mapper;

import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = ProductMapper.class)
public interface CartMapper {

    CartResponse toDto(Cart cart);


}
