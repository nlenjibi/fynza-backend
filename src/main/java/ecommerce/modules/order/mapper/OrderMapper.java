package ecommerce.modules.order.mapper;

import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.OrderItem;
import ecommerce.modules.order.entity.OrderStats;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.user.entity.User;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    Random RANDOM = new SecureRandom();

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "items",      source = "orderItems")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    default Page<OrderResponse> toResponsePage(Page<Order> orderPage) {
        return orderPage.map(this::toResponse);
    }

    @Mapping(target = "productId", expression = "java(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null)")
    OrderItemResponse toItemResponse(OrderItem orderItem);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> orderItems);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "orderNumber",   expression = "java(generateOrderNumber())")
    @Mapping(target = "customer",      source = "user")
    @Mapping(target = "status",        constant = "PENDING")
    @Mapping(target = "paymentStatus", constant = "PENDING")
    @Mapping(target = "orderItems",    expression = "java(mapItems(request.getItems()))")
    @Mapping(target = "isActive",      ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "shippingAddress", ignore = true) // Handled in service as it's an entity
    Order toEntity(OrderCreateRequest request, User user);

    default List<OrderItem> mapItems(List<OrderItemCreateRequest> items) {
        if (items == null || items.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return items.stream()
                .map(this::toOrderItemEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "order",      ignore = true)
    @Mapping(target = "product",    source = "productId")
    OrderItem toOrderItemEntity(OrderItemCreateRequest request);

    default Product map(UUID productId) {
        if (productId == null) return null;
        return Product.builder().id(productId).build();
    }
    
    default Product map(Long productId) { // For cases where Long might still be used
        if (productId == null) return null;
        return Product.builder().id(UUID.randomUUID()).build(); // Placeholder or error
    }

    default String generateOrderNumber() {
        String date   = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        String random = String.format("%06d", RANDOM.nextInt(999_999));
        return "ORD-" + date + "-" + random;
    }
}
