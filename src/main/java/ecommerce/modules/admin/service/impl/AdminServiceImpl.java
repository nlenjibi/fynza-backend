package ecommerce.modules.admin.service.impl;

import ecommerce.common.enums.InventoryStatus;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.Role;
import ecommerce.modules.admin.dto.AdminAnalyticsDto;
import ecommerce.modules.admin.dto.AdminDashboardDto;
import ecommerce.modules.admin.dto.ContentAnalyticsDto;
import ecommerce.modules.admin.service.AdminService;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.order.dto.OrderDashboardDto;
import ecommerce.modules.order.repository.OrderItemRepository;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin service implementation.
 * Handles admin dashboard and analytics.
 * 
 * Note: Order-related methods delegate to OrderService for consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Cacheable(value = "admin-dashboard", key = "'stats'")
    public AdminDashboardDto getDashboardStats() {
        log.info("Calculating dashboard statistics");

        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalSellers = userRepository.countByRole(Role.SELLER);
        long totalProducts = productRepository.count();
        long lowStockProducts = productRepository.countByInventoryStatusAndIsActiveTrue(InventoryStatus.LOW_STOCK);

        OrderDashboardDto orderDashboard = orderService.getOrderDashboard();
        List<AdminDashboardDto.RecentOrderDto> recentOrders = orderDashboard.getRecentOrders().stream()
                .map(this::convertRecentOrderDto)
                .collect(Collectors.toList());

        List<AdminDashboardDto.TopSellerDto> topSellers = getTopSellers(5);
        List<AdminDashboardDto.LowStockAlertDto> lowStockAlerts = getLowStockAlerts(10);

        return AdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalSellers(totalSellers)
                .totalOrders(orderDashboard.getTotalOrders())
                .totalProducts(totalProducts)
                .totalRevenue(orderDashboard.getTotalRevenue())
                .pendingOrders(orderDashboard.getPendingOrders())
                .activeUsers(userRepository.countActiveUsers())
                .lowStockProducts(lowStockProducts)
                .ordersByStatus(orderDashboard.getOrdersByStatus())
                .recentOrders(recentOrders)
                .topSellers(topSellers)
                .lowStockAlerts(lowStockAlerts)
                .orderCompletionRate(orderDashboard.getOrderCompletionRate())
                .cancellationRate(orderDashboard.getCancellationRate())
                .build();
    }

    @Override
    @Cacheable(value = "admin-analytics", key = "#filterPeriod")
    public AdminAnalyticsDto getAnalytics(String filterPeriod) {
        log.info("Getting admin analytics for period: {}", filterPeriod);
        return orderService.getAdminAnalytics(filterPeriod);
    }

    private AdminDashboardDto.RecentOrderDto convertRecentOrderDto(OrderDashboardDto.RecentOrderDto source) {
        return AdminDashboardDto.RecentOrderDto.builder()
                .orderId(source.getOrderId())
                .orderNumber(source.getOrderNumber())
                .customerName(source.getCustomerName())
                .sellerName(source.getSellerName())
                .amount(source.getAmount())
                .status(source.getStatus())
                .createdAt(source.getCreatedAt())
                .build();
    }

    private List<AdminDashboardDto.TopSellerDto> getTopSellers(int limit) {
        List<Object[]> topSellerIds = orderItemRepository.findTopSellerIdsByOrderCount(PageRequest.of(0, limit));
        List<AdminDashboardDto.TopSellerDto> topSellers = new ArrayList<>();
        
        for (Object[] result : topSellerIds) {
            UUID sellerId = (UUID) result[0];
            Long orderCount = (Long) result[1];
            
            User seller = userRepository.findById(sellerId).orElse(null);
            if (seller != null) {
                BigDecimal revenue = orderItemRepository.sumRevenueBySellerId(sellerId);
                long productCount = productRepository.findBySellerId(UUID.fromString("00000000-0000-0000-0000-000000000000"), PageRequest.of(0, 1000))
                        .getContent().stream()
                        .filter(p -> p.getSeller() != null && p.getSeller().getId().equals(sellerId))
                        .count();
                
                topSellers.add(AdminDashboardDto.TopSellerDto.builder()
                        .sellerId(sellerId.toString())
                        .sellerName(seller.getEmail())
                        .productCount(productCount)
                        .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                        .orderCount(orderCount)
                        .build());
            }
        }
        return topSellers;
    }

    private List<AdminDashboardDto.LowStockAlertDto> getLowStockAlerts(int limit) {
        List<Product> lowStockProducts = productRepository.findByStatus(
                ecommerce.common.enums.ProductStatus.ACTIVE, 
                PageRequest.of(0, limit * 2))
                .getContent();
        
        return lowStockProducts.stream()
                .filter(p -> p.getInventoryStatus() == InventoryStatus.LOW_STOCK || 
                           p.getInventoryStatus() == InventoryStatus.OUT_OF_STOCK)
                .filter(p -> p.getIsActive())
                .limit(limit)
                .map(product -> AdminDashboardDto.LowStockAlertDto.builder()
                        .productId(product.getId().toString())
                        .productName(product.getName())
                        .sku(product.getSku())
                        .sellerName(product.getSeller() != null ? product.getSeller().getEmail() : "N/A")
                        .currentStock(product.getStock() != null ? product.getStock() : 0)
                        .build())
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "admin-dashboard", allEntries = true)
    @Scheduled(fixedRate = 300_000)
    public void clearDashboardCache() {
        log.debug("Clearing admin dashboard cache");
    }

    @Override
    public ContentAnalyticsDto getContentAnalytics(String filterPeriod, String contentType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime previousStartDate;
        int monthCount;

        switch (filterPeriod != null ? filterPeriod.toLowerCase() : "month") {
            case "week":
                startDate = now.minusWeeks(1);
                previousStartDate = now.minusWeeks(2);
                monthCount = 1;
                break;
            case "year":
                startDate = now.minusYears(1);
                previousStartDate = now.minusYears(2);
                monthCount = 12;
                break;
            case "month":
            default:
                startDate = now.minusMonths(1);
                previousStartDate = now.minusMonths(2);
                monthCount = 1;
                break;
        }

        long totalProducts = productRepository.count();
        long totalCategories = categoryRepository.count();
        long totalOrders = orderService.getOrderDashboard().getTotalOrders();
        long activeSellers = userRepository.countByRole(Role.SELLER);
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);

        List<Product> products = productRepository.findAll();
        long articlesPublished = products.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(startDate))
                .count();

        long previousProducts = products.stream()
                .filter(p -> p.getCreatedAt() != null && 
                        p.getCreatedAt().isAfter(previousStartDate) && 
                        p.getCreatedAt().isBefore(startDate))
                .count();

        List<ContentAnalyticsDto.MonthlyContentMetric> monthlyTrends = new ArrayList<>();
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        for (int i = monthCount - 1; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            long productsCreated = products.stream()
                    .filter(p -> p.getCreatedAt() != null && 
                            p.getCreatedAt().isAfter(monthStart) && 
                            p.getCreatedAt().isBefore(monthEnd))
                    .count();
            
            monthlyTrends.add(ContentAnalyticsDto.MonthlyContentMetric.builder()
                    .month(monthNames[monthStart.getMonthValue() - 1])
                    .productsCreated(productsCreated)
                    .categoriesCreated(0L)
                    .ordersPlaced(0L)
                    .newCustomers(0L)
                    .build());
        }

        List<ContentAnalyticsDto.CategoryEngagement> categoryEngagement = new ArrayList<>();
        productRepository.findAll().stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getName()))
                .forEach((categoryName, categoryProducts) -> {
                    long productCount = categoryProducts.size();
                    BigDecimal avgPrice = categoryProducts.stream()
                            .map(Product::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(productCount), 2, RoundingMode.HALF_UP);
                    
                    categoryEngagement.add(ContentAnalyticsDto.CategoryEngagement.builder()
                            .categoryName(categoryName)
                            .productCount(productCount)
                            .avgPrice(avgPrice.doubleValue())
                            .orderCount(0L)
                            .build());
                });

        List<ContentAnalyticsDto.TopContent> topContent = products.stream()
                .sorted((p1, p2) -> {
                    long v1 = p1.getViewCount() != null ? p1.getViewCount() : 0;
                    long v2 = p2.getViewCount() != null ? p2.getViewCount() : 0;
                    return Long.compare(v2, v1);
                })
                .limit(5)
                .map(product -> {
                    long views = product.getViewCount() != null ? product.getViewCount() : 0;
                    long clicks = (long) (views * 0.27);
                    double ctr = views > 0 ? (double) clicks / views * 100 : 0;
                    return ContentAnalyticsDto.TopContent.builder()
                            .title(product.getName())
                            .type("Product")
                            .views(views)
                            .clicks(clicks)
                            .ctr(ctr)
                            .trend(views > 1000 ? "up" : "down")
                            .build();
                })
                .collect(Collectors.toList());

        Double productsChange = calculateGrowth((double) previousProducts, (double) articlesPublished);

        return ContentAnalyticsDto.builder()
                .filterPeriod(filterPeriod != null ? filterPeriod : "month")
                .startDate(startDate.toLocalDate().toString())
                .endDate(now.toLocalDate().toString())
                .totalProducts(totalProducts)
                .productsChange(productsChange)
                .totalCategories(totalCategories)
                .categoriesChange(0.0)
                .totalOrders(totalOrders)
                .ordersChange(0.0)
                .activeSellers(activeSellers)
                .sellersChange(0.0)
                .totalCustomers(totalCustomers)
                .customersChange(0.0)
                .articlesPublished(articlesPublished)
                .articlesChange(productsChange)
                .monthlyTrends(monthlyTrends)
                .categoryEngagement(categoryEngagement)
                .topContent(topContent)
                .build();
    }

    private Double calculateGrowth(double previous, double current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100;
    }
}
