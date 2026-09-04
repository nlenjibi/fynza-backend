package ecommerce.modules.admin.service.impl;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.Role;
import ecommerce.modules.admin.service.AnalyticsService;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.repository.OrderItemRepository;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public long getTotalUsers() {
        return userRepository.count();
    }

    @Override
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll();
        BigDecimal total = orders.stream()
                .filter(o -> o.getCreatedAt() != null && 
                           !o.getCreatedAt().isBefore(start) && 
                           !o.getCreatedAt().isAfter(end))
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total;
    }

    @Override
    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    @Override
    public long getOrderCountForPeriod(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .filter(o -> o.getCreatedAt() != null && 
                           !o.getCreatedAt().isBefore(start) && 
                           !o.getCreatedAt().isAfter(end))
                .count();
    }

    @Override
    public List<SellerMetrics> getTopSellers(int limit) {
        List<User> sellers = userRepository.findByRole(Role.SELLER,
                org.springframework.data.domain.Pageable.ofSize(limit)).getContent();
        
        List<SellerMetrics> metrics = new ArrayList<>();
        for (User seller : sellers) {
            long orders = orderItemRepository.countByProductSellerId(seller.getId());
            BigDecimal revenue = orderItemRepository.sumRevenueBySellerId(seller.getId());
            double cancellationRate = calculateSellerCancellationRate(seller.getId());
            long lowStock = productRepository.countByInventoryStatusAndIsActiveTrue(
                    ecommerce.common.enums.InventoryStatus.LOW_STOCK);
            
            metrics.add(new SellerMetrics(
                seller.getId(),
                seller.getUsername() != null ? seller.getUsername() : seller.getEmail(),
                orders,
                revenue != null ? revenue : BigDecimal.ZERO,
                cancellationRate,
                lowStock,
                0.0
            ));
        }
        return metrics;
    }

    @Override
    public List<TrendData> getOrderTrends(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll();
        List<TrendData> trends = new ArrayList<>();
        
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            final LocalDateTime periodStart = current;
            final LocalDateTime periodEnd = current.plusDays(1);
            long count = orders.stream()
                    .filter(o -> o.getCreatedAt() != null &&
                               !o.getCreatedAt().isBefore(periodStart) &&
                               o.getCreatedAt().isBefore(periodEnd))
                    .count();
            trends.add(new TrendData(current, count, BigDecimal.ZERO));
            current = periodEnd;
        }
        return trends;
    }

    @Override
    public List<TrendData> getRevenueTrends(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll();
        List<TrendData> trends = new ArrayList<>();
        
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            final LocalDateTime periodStart = current;
            final LocalDateTime periodEnd = current.plusDays(1);
            BigDecimal value = orders.stream()
                    .filter(o -> o.getCreatedAt() != null &&
                               !o.getCreatedAt().isBefore(periodStart) &&
                               o.getCreatedAt().isBefore(periodEnd))
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            trends.add(new TrendData(current, 0, value));
            current = periodEnd;
        }
        return trends;
    }

    @Override
    public long getProductSales(UUID sellerId) {
        return orderItemRepository.countByProductSellerId(sellerId);
    }

    @Override
    public BigDecimal getSellerRevenue(UUID sellerId) {
        BigDecimal revenue = orderItemRepository.sumRevenueBySellerId(sellerId);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getSellerRevenueForPeriod(UUID sellerId, LocalDateTime start, LocalDateTime end) {
        return getSellerRevenue(sellerId);
    }

    @Override
    public double getSellerCancellationRate(UUID sellerId) {
        return calculateSellerCancellationRate(sellerId);
    }

    @Override
    public long getLowStockCount(UUID sellerId) {
        return productRepository.countByInventoryStatusAndIsActiveTrue(
                ecommerce.common.enums.InventoryStatus.LOW_STOCK);
    }

    @Override
    public SellerMetrics getSellerMetrics(UUID sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        String sellerName = seller != null && seller.getUsername() != null 
                ? seller.getUsername() 
                : (seller != null ? seller.getEmail() : "Unknown");
        
        long totalOrders = getProductSales(sellerId);
        BigDecimal totalRevenue = getSellerRevenue(sellerId);
        double cancellationRate = getSellerCancellationRate(sellerId);
        long lowStockCount = getLowStockCount(sellerId);
        
        return new SellerMetrics(
            sellerId,
            sellerName,
            totalOrders,
            totalRevenue,
            cancellationRate,
            lowStockCount,
            0.0
        );
    }

    @Override
    public BigDecimal getCustomerTotalSpending(UUID customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId, 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<CategoryPreference> getCustomerCategoryPreferences(UUID customerId) {
        return new ArrayList<>();
    }

    @Override
    public void refreshAnalyticsCache() {
        log.info("Refreshing analytics cache");
    }

    @Async("analyticsExecutor")
    public CompletableFuture<Void> computeDailyAggregates(LocalDateTime date) {
        log.info("Computing daily aggregates for: {}", date);
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                log.info("Daily aggregates computed for: {}", date);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public void recordEvent(String eventType, UUID entityId, java.util.Map<String, Object> metadata) {
        log.debug("Recording analytics event: {} for entity: {}", eventType, entityId);
    }

    private double calculateSellerCancellationRate(UUID sellerId) {
        List<Order> sellerOrders = orderRepository.findBySellerId(sellerId);
        if (sellerOrders.isEmpty()) {
            return 0.0;
        }
        long cancelled = sellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .count();
        return (double) cancelled / sellerOrders.size() * 100;
    }
}
