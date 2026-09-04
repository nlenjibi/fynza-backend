package ecommerce.modules.cart.service.impl;

import ecommerce.common.exception.InsufficientStockException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.entity.CartItem;
import ecommerce.modules.cart.entity.StockReservation;
import ecommerce.modules.cart.repository.CartItemRepository;
import ecommerce.modules.cart.repository.CartRepository;
import ecommerce.modules.cart.repository.StockReservationRepository;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.coupon.entity.Coupon;
import ecommerce.modules.coupon.repository.CouponRepository;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.entity.ProductImage;
import ecommerce.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final int RESERVATION_MINUTES = 15;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        log.info("Fetching cart for user: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartById(UUID cartId, UUID userId) {
        log.info("Fetching cart by id: {} for user: {}", cartId, userId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Cart", cartId));
        if (!cart.getUserId().equals(userId)) {
            throw new IllegalStateException("Cart does not belong to user");
        }
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartItemResponse addItem(UUID userId, AddToCartRequest request) {
        log.info("Adding item to cart for user: {}, product: {}, quantity: {}", 
                userId, request.getProductId(), request.getQuantity());
        
        Cart cart = getOrCreateCart(userId);
        
        var product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));
        
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        
        return addToCart(cart, product, quantity);
    }

    @Override
    @Transactional
    public CartItemResponse addItemByProductId(UUID userId, UUID productId, int quantity) {
        log.info("Adding item to cart for user: {}, product: {}, quantity: {}", userId, productId, quantity);
        
        Cart cart = getOrCreateCart(userId);
        
        var product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));
        
        return addToCart(cart, product, quantity);
    }

    private CartItemResponse addToCart(Cart cart, Product product, int quantity) {
        int availableStock = (product.getStock() != null ? product.getStock() : 0) 
                - (product.getReservedQuantity() != null ? product.getReservedQuantity() : 0);
        if (availableStock < quantity) {
            throw new InsufficientStockException(product.getName(), availableStock, quantity);
        }
        
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);
        
        BigDecimal price = calculateDiscountPrice(product);
        
        if (cartItem != null) {
            int newQuantity = cartItem.getQuantity() + quantity;
            int newAvailableStock = (product.getStock() != null ? product.getStock() : 0) 
                    - (product.getReservedQuantity() != null ? product.getReservedQuantity() : 0);
            if (newAvailableStock < newQuantity) {
                throw new InsufficientStockException(product.getName(), newAvailableStock, newQuantity);
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setPrice(price);
            cartItem = cartItemRepository.save(cartItem);
        } else {
            cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .price(price)
                    .build();
            cartItem = cartItemRepository.save(cartItem);
        }
        
        createStockReservation(cartItem, product, quantity);
        
        return mapToCartItemResponse(cartItem);
    }

    @Override
    @Transactional
    public CartItemResponse updateItemQuantity(UUID userId, UUID cartItemId, int quantity) {
        log.info("Updating cart item quantity for user: {}, cartItem: {}, quantity: {}", 
                userId, cartItemId, quantity);
        
        Cart cart = getOrCreateCart(userId);
        
        CartItem cartItem = cartItemRepository.findByCartIdAndId(cart.getId(), cartItemId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Cart item", cartItemId));
        
        return updateCartItem(cartItem, quantity);
    }

    @Override
    @Transactional
    public CartItemResponse updateItemByProductId(UUID userId, UUID productId, int quantity) {
        log.info("Updating cart item by productId for user: {}, productId: {}, quantity: {}", 
                userId, productId, quantity);
        
        Cart cart = getOrCreateCart(userId);
        
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Cart item for product", productId));
        
        return updateCartItem(cartItem, quantity);
    }

    private CartItemResponse updateCartItem(CartItem cartItem, int quantity) {
        var product = cartItem.getProduct();
        int availableStock = (product.getStock() != null ? product.getStock() : 0) 
                - (product.getReservedQuantity() != null ? product.getReservedQuantity() : 0);
        
        int existingReserved = 0;
        StockReservation reservation = stockReservationRepository.findByCartItemId(cartItem.getId()).orElse(null);
        if (reservation != null) {
            existingReserved = reservation.getQuantity();
        }
        
        int netAvailable = availableStock + existingReserved;
        if (netAvailable < quantity) {
            throw new InsufficientStockException(product.getName(), netAvailable, quantity);
        }
        
        cartItem.setQuantity(quantity);
        cartItem = cartItemRepository.save(cartItem);
        
        if (reservation != null) {
            reservation.setQuantity(quantity);
            reservation.setExpiresAt(LocalDateTime.now().plusMinutes(RESERVATION_MINUTES));
            stockReservationRepository.save(reservation);
        }
        
        return mapToCartItemResponse(cartItem);
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID cartItemId) {
        log.info("Removing item from cart for user: {}, cartItem: {}", userId, cartItemId);
        
        Cart cart = getOrCreateCart(userId);
        
        CartItem cartItem = cartItemRepository.findByCartIdAndId(cart.getId(), cartItemId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Cart item", cartItemId));
        
        removeCartItem(cartItem);
    }

    @Override
    @Transactional
    public void removeItemByProductId(UUID userId, UUID productId) {
        log.info("Removing item from cart for user: {}, productId: {}", userId, productId);
        
        Cart cart = getOrCreateCart(userId);
        
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Cart item for product", productId));
        
        removeCartItem(cartItem);
    }

    private void removeCartItem(CartItem cartItem) {
        stockReservationRepository.findByCartItemId(cartItem.getId())
                .ifPresent(reservation -> {
                    stockReservationRepository.delete(reservation);
                    releaseStockReservation(cartItem.getProduct(), reservation.getQuantity());
                });
        
        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public CartResponse applyCoupon(UUID userId, String couponCode) {
        log.info("Applying coupon for user: {}, coupon: {}", userId, couponCode);
        
        Cart cart = getOrCreateCart(userId);
        
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + couponCode));
        
        validateCoupon(coupon, cart);
        
        cart.setCouponCode(couponCode);
        cartRepository.save(cart);
        
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);
        
        Cart cart = getOrCreateCart(userId);
        
        var cartItems = cartItemRepository.findByCartId(cart.getId());
        
        for (CartItem item : cartItems) {
            stockReservationRepository.findByCartItemId(item.getId())
                    .ifPresent(reservation -> {
                        stockReservationRepository.delete(reservation);
                        releaseStockReservation(item.getProduct(), reservation.getQuantity());
                    });
        }
        
        cartItemRepository.deleteByCartId(cart.getId());
        
        cart.setCouponCode(null);
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public CartResponse createCart(UUID userId) {
        log.info("Creating cart for user: {}", userId);
        
        Cart existingCart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (existingCart != null) {
            return mapToCartResponse(existingCart);
        }
        
        Cart newCart = Cart.builder()
                .userId(userId)
                .build();
        newCart = cartRepository.save(newCart);
        
        return mapToCartResponse(newCart);
    }

    @Override
    @Transactional
    public CartResponse mergeCart(UUID userId, UUID guestCartId) {
        log.info("Merging guest cart: {} with user: {}", guestCartId, userId);
        
        Cart userCart = getOrCreateCart(userId);
        
        Cart guestCart = cartRepository.findById(guestCartId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Guest cart", guestCartId));
        
        var guestItems = cartItemRepository.findByCartId(guestCartId);
        
        for (CartItem guestItem : guestItems) {
            var product = guestItem.getProduct();
            int quantity = guestItem.getQuantity();
            
            CartItem existingItem = cartItemRepository.findByCartIdAndProductId(userCart.getId(), product.getId())
                    .orElse(null);
            
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                cartItemRepository.save(existingItem);
                
                createStockReservation(existingItem, product, quantity);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .product(product)
                        .quantity(quantity)
                        .price(guestItem.getPrice())
                        .build();
                newItem = cartItemRepository.save(newItem);
                
                createStockReservation(newItem, product, quantity);
            }
        }
        
        cartItemRepository.deleteByCartId(guestCartId);
        cartRepository.delete(guestCart);
        
        return mapToCartResponse(userCart);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private void createStockReservation(CartItem cartItem, Product product, int quantity) {
        StockReservation reservation = StockReservation.builder()
                .cartItem(cartItem)
                .product(product)
                .quantity(quantity)
                .expiresAt(LocalDateTime.now().plusMinutes(RESERVATION_MINUTES))
                .build();
        stockReservationRepository.save(reservation);
        
        productRepository.reserveStockAndIsActiveTrue(product.getId(), quantity);
    }

    private void releaseStockReservation(Product product, int quantity) {
        productRepository.releaseReservedStockAndIsActiveTrue(product.getId(), quantity);
    }

    private void validateCoupon(Coupon coupon, Cart cart) {
        if (coupon.getStatus() != ecommerce.common.enums.CouponStatus.ACTIVE) {
            throw new IllegalStateException("Coupon is not active");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new IllegalStateException("Coupon is expired or not yet valid");
        }
        
        if (coupon.getMaxUses() != null && coupon.getUsageCount() >= coupon.getMaxUses()) {
            throw new IllegalStateException("Coupon usage limit reached");
        }
        
        BigDecimal subtotal = calculateSubtotal(cart);
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalStateException("Minimum order amount not met");
        }
    }

    private CartResponse mapToCartResponse(Cart cart) {
        var cartItems = cartItemRepository.findByCartId(cart.getId());
        
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal[] discount = {BigDecimal.ZERO};
        
        if (cart.getCouponCode() != null) {
            final BigDecimal finalSubtotal = subtotal;
            couponRepository.findByCode(cart.getCouponCode()).ifPresent(coupon -> {
                if (coupon.getDiscountType() == ecommerce.common.enums.DiscountType.PERCENTAGE) {
                    discount[0] = finalSubtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
                } else {
                    discount[0] = coupon.getDiscountValue();
                }
            });
        }
        
        BigDecimal tax = subtotal.subtract(discount[0]).multiply(BigDecimal.valueOf(0.1));
        BigDecimal shippingCost = subtotal.compareTo(BigDecimal.valueOf(50)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(5.99);
        BigDecimal total = subtotal.subtract(discount[0]).add(tax).add(shippingCost);
        
        var itemResponses = new ArrayList<CartItemResponse>();
        for (CartItem item : cartItems) {
            itemResponses.add(mapToCartItemResponse(item));
        }
        
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(shippingCost)
                .discount(discount[0])
                .totalPrice(total)
                .itemsCount(itemResponses.size())
                .couponCode(cart.getCouponCode())
                .build();
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        var cartItems = cartItemRepository.findByCartId(cart.getId());
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            subtotal = subtotal.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }

    private BigDecimal calculateDiscountPrice(Product product) {
        if (product.getDiscount() != null && product.getDiscount().compareTo(BigDecimal.ZERO) > 0 
                && product.getOriginalPrice() != null) {
            return product.getOriginalPrice()
                    .subtract(product.getOriginalPrice()
                            .multiply(product.getDiscount())
                            .divide(BigDecimal.valueOf(100)));
        }
        return product.getPrice();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discount(product.getDiscount())
                .stock(product.getStock())
                .images(product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .toList())
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        StockReservation reservation = stockReservationRepository.findByCartItemId(cartItem.getId()).orElse(null);
        
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .product(mapToProductResponse(cartItem.getProduct()))
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .reserved(reservation != null && !reservation.isExpired())
                .reservationExpiresAt(reservation != null ? reservation.getExpiresAt() : null)
                .build();
    }
}
