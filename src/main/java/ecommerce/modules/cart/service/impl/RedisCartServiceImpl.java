package ecommerce.modules.cart.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.cart.dto.CartItemData;
import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.cart.service.RedisCartService;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RedisCartServiceImpl implements RedisCartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofMinutes(30);

    @Override
    @Transactional
    public CartItemResponse addItem(UUID userId, UUID productId, int quantity) {
        String cartKey = getCartKey(userId);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductResponse product = productService.findById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        // Stock check delegated to inventory module — skipped until wired
        CartItemData item = getCartItem(cartKey, productId);

        if (item != null) {
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            item = CartItemData.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .price(null) // Price sourced from pricing module once wired
                    .productName(product.getName())
                    .productImage(null) // Image sourced from media module once wired
                    .build();
        }

        saveCartItem(cartKey, item);
        refreshTTL(cartKey);

        log.info("Added item to cart for user {}: product {}, quantity {}", userId, productId, quantity);
        return mapToResponse(item);
    }

    @Override
    public CartResponse getCart(UUID userId) {
        String cartKey = getCartKey(userId);

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);

        if (entries.isEmpty()) {
            return emptyCartResponse(userId);
        }

        List<CartItemResponse> items = entries.values().stream()
                .map(value -> {
                    try {
                        CartItemData item = objectMapper.convertValue(value, CartItemData.class);
                        return mapToResponse(item);
                    } catch (Exception e) {
                        log.error("Error parsing cart item: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        refreshTTL(cartKey);
        return buildCartResponse(userId, items);
    }

    @Override
    @Transactional
    public CartItemResponse updateItemQuantity(UUID userId, UUID productId, int quantity) {
        String cartKey = getCartKey(userId);

        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        CartItemData item = getCartItem(cartKey, productId);
        if (item == null) {
            throw new ResourceNotFoundException("Item not found in cart");
        }

        if (quantity == 0) {
            removeItem(userId, productId);
            return null;
        }

        // Stock check delegated to inventory module — skipped until wired
        item.setQuantity(quantity);
        saveCartItem(cartKey, item);
        refreshTTL(cartKey);

        log.info("Updated cart item for user {}: product {}, quantity {}", userId, productId, quantity);
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID productId) {
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().delete(cartKey, productId.toString());
        refreshTTL(cartKey);

        log.info("Removed item from cart for user {}: product {}", userId, productId);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        String cartKey = getCartKey(userId);
        redisTemplate.delete(cartKey);

        log.info("Cleared cart for user {}", userId);
    }

    @Override
    public boolean hasProduct(UUID userId, UUID productId) {
        String cartKey = getCartKey(userId);
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(cartKey, productId.toString()));
    }

    @Override
    public long getItemCount(UUID userId) {
        String cartKey = getCartKey(userId);
        Long size = redisTemplate.opsForHash().size(cartKey);
        return size != null ? size : 0;
    }

    private String getCartKey(UUID userId) {
        return CART_KEY_PREFIX + userId;
    }

    private CartItemData getCartItem(String cartKey, UUID productId) {
        Object value = redisTemplate.opsForHash().get(cartKey, productId.toString());
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, CartItemData.class);
    }

    private void saveCartItem(String cartKey, CartItemData item) {
        try {
            redisTemplate.opsForHash().put(cartKey, item.getProductId().toString(), item);
        } catch (Exception e) {
            log.error("Error saving cart item: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save cart item", e);
        }
    }

    private void refreshTTL(String cartKey) {
        redisTemplate.expire(cartKey, CART_TTL);
    }

    private CartItemResponse mapToResponse(CartItemData item) {
        BigDecimal unitPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        BigDecimal lineTotal = item.getQuantity() != null
                ? unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                : BigDecimal.ZERO;

        return CartItemResponse.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .build();
    }

    private CartResponse emptyCartResponse(UUID userId) {
        return CartResponse.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .itemsCount(0)
                .grandTotal(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .shippingTotal(BigDecimal.ZERO)
                .taxTotal(BigDecimal.ZERO)
                .hasPriceChanges(false)
                .build();
    }

    private CartResponse buildCartResponse(UUID userId, List<CartItemResponse> items) {
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .userId(userId)
                .items(items)
                .itemsCount(items.size())
                .subtotal(subtotal)
                .grandTotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .shippingTotal(BigDecimal.ZERO)
                .taxTotal(BigDecimal.ZERO)
                .hasPriceChanges(false)
                .build();
    }
}
