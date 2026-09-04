package ecommerce.modules.cart.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.exception.InsufficientStockException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.cart.dto.CartItemData;
import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.cart.service.RedisCartService;
import ecommerce.modules.product.dto.ProductResponse;
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

        int availableStock = product.getStock() != null ? product.getStock() : 0;
        if (availableStock < quantity) {
            throw new InsufficientStockException(product.getName(), availableStock, quantity);
        }

        CartItemData item = getCartItem(cartKey, productId);
        
        if (item != null) {
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > availableStock) {
                throw new InsufficientStockException(product.getName(), availableStock, newQuantity);
            }
            item.setQuantity(newQuantity);
        } else {
            item = CartItemData.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .price(product.getPrice())
                    .productName(product.getName())
                    .productImage(product.getImages() != null && !product.getImages().isEmpty() 
                            ? product.getImages().get(0) : null)
                    .build();
        }

        saveCartItem(cartKey, item);
        refreshTTL(cartKey);

        log.info("Added item to cart for user {}: product {}, quantity {}", userId, productId, quantity);
        return mapToResponse(item, product);
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
                        ProductResponse product = getProductSafe(item.getProductId());
                        return mapToResponse(item, product);
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

        ProductResponse product = getProductSafe(productId);
        if (product != null && quantity > (product.getStock() != null ? product.getStock() : 0)) {
            throw new InsufficientStockException(product.getName(), 
                    product.getStock() != null ? product.getStock() : 0, quantity);
        }

        item.setQuantity(quantity);
        saveCartItem(cartKey, item);
        refreshTTL(cartKey);

        log.info("Updated cart item for user {}: product {}, quantity {}", userId, productId, quantity);
        return mapToResponse(item, product);
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

    private ProductResponse getProductSafe(UUID productId) {
        try {
            return productService.findById(productId);
        } catch (Exception e) {
            log.warn("Product {} not found, returning minimal data", productId);
            return null;
        }
    }

    private CartItemResponse mapToResponse(CartItemData item, ProductResponse product) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (item.getPrice() != null && item.getQuantity() != null) {
            totalPrice = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        }
        
        return CartItemResponse.builder()
                .quantity(item.getQuantity())
                .product(product)
                .totalPrice(totalPrice)
                .build();
    }

    private CartResponse emptyCartResponse(UUID userId) {
        return CartResponse.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .itemsCount(0)
                .totalPrice(BigDecimal.ZERO)
                .build();
    }

    private CartResponse buildCartResponse(UUID userId, List<CartItemResponse> items) {
        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .userId(userId)
                .items(items)
                .itemsCount(items.size())
                .totalPrice(totalPrice)
                .build();
    }
}
