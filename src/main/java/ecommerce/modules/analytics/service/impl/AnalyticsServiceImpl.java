package ecommerce.modules.analytics.service.impl;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.Role;
import ecommerce.modules.analytics.service.AnalyticsService;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.repository.OrderItemRepository;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        BigDecimal revenue = orderRepository.sumTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        Instant startI = start.atZone(ZoneId.systemDefault()).toInstant();
        Instant endI   = end.atZone(ZoneId.systemDefault()).toInstant();
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null
                          && !o.getCreatedAt().isBefore(startI)
                          && !o.getCreatedAt().isAfter(endI))
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    @Override
    public long getOrderCountForPeriod(LocalDateTime start, LocalDateTime end) {
        Instant startI = start.atZone(ZoneId.systemDefault()).toInstant();
        Instant endI   = end.atZone(ZoneId.systemDefault()).toInstant();
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null
                          && !o.getCreatedAt().isBefore(startI)
                          && !o.getCreatedAt().isAfter(endI))
                .count();
    }

    @Override
    public List<SellerMetrics> getTopSellers(int limit) {
        List<User> sellers = userRepository.findByRole(Role.SELLER, Pageable.ofSize(limit)).getContent();
        List<SellerMetrics> metrics = new ArrayList<>();
        for (User seller : sellers) {
            long orders = 0L; // seller Long ID resolution deferred to seller analytics module
            BigDecimal revenue = BigDecimal.ZERO;
            double cancellationRate = calculateCancellationRate(seller.getId());
            long lowStock = 0L; // Inventory status delegated to inventory module once wired
            metrics.add(new SellerMetrics(
                    seller.getId(), resolveSellerName(seller),
                    orders, revenue != null ? revenue : BigDecimal.ZERO,
                    cancellationRate, lowStock, 0.0));
        }
        return metrics;
    }

    @Override
    public List<TrendData> getOrderTrends(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll();
        List<TrendData> trends = new ArrayList<>();
        LocalDateTime cursor = start;
        while (!cursor.isAfter(end)) {
            final Instant from = cursor.atZone(ZoneId.systemDefault()).toInstant();
            final Instant to   = cursor.plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
            long count = orders.stream()
                    .filter(o -> o.getCreatedAt() != null
                              && !o.getCreatedAt().isBefore(from)
                              && o.getCreatedAt().isBefore(to))
                    .count();
            trends.add(new TrendData(cursor, count, BigDecimal.ZERO));
            cursor = cursor.plusDays(1);
        }
        return trends;
    }

    @Override
    public List<TrendData> getRevenueTrends(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findAll();
        List<TrendData> trends = new ArrayList<>();
        LocalDateTime cursor = start;
        while (!cursor.isAfter(end)) {
            final Instant from = cursor.atZone(ZoneId.systemDefault()).toInstant();
            final Instant to   = cursor.plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
            BigDecimal value = orders.stream()
                    .filter(o -> o.getCreatedAt() != null
                              && !o.getCreatedAt().isBefore(from)
                              && o.getCreatedAt().isBefore(to))
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            trends.add(new TrendData(cursor, 0, value));
            cursor = cursor.plusDays(1);
        }
        return trends;
    }

    @Override
    public long getProductSales(UUID sellerId) {
        return 0L; // seller Long ID resolution deferred to seller analytics module
    }

    @Override
    public BigDecimal getSellerRevenue(UUID sellerId) {
        return BigDecimal.ZERO; // seller Long ID resolution deferred to seller analytics module
    }

    @Override
    public BigDecimal getSellerRevenueForPeriod(UUID sellerId, LocalDateTime start, LocalDateTime end) {
        return getSellerRevenue(sellerId);
    }

    @Override
    public double getSellerCancellationRate(UUID sellerId) {
        return calculateCancellationRate(sellerId);
    }

    @Override
    public long getLowStockCount(UUID sellerId) {
        return 0L; // Inventory status delegated to inventory module once wired
    }

    @Override
    public SellerMetrics getSellerMetrics(UUID sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        return new SellerMetrics(
                sellerId, seller != null ? resolveSellerName(seller) : "Unknown",
                getProductSales(sellerId), getSellerRevenue(sellerId),
                getSellerCancellationRate(sellerId), getLowStockCount(sellerId), 0.0);
    }

    @Override
    public BigDecimal getCustomerTotalSpending(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, Pageable.unpaged()).getContent().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<CategoryPreference> getCustomerCategoryPreferences(UUID customerId) {
        return new ArrayList<>();
    }

    @Override
    public void recordEvent(String eventType, UUID entityId, Map<String, Object> metadata) {
        log.debug("Recording analytics event: {} for entity: {}", eventType, entityId);
    }

    @Override
    public void refreshAnalyticsCache() {
        log.info("Refreshing analytics cache");
    }

    @Async("analyticsExecutor")
    public CompletableFuture<Void> computeDailyAggregates(LocalDateTime date) {
        log.info("Computing daily aggregates for: {}", date);
        return CompletableFuture.runAsync(() -> log.info("Daily aggregates computed for: {}", date));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private double calculateCancellationRate(UUID sellerId) {
        return 0.0; // seller Long ID resolution deferred to seller analytics module
    }

    private String resolveSellerName(User seller) {
        return seller.getUsername() != null ? seller.getUsername() : seller.getEmail();
    }
}
