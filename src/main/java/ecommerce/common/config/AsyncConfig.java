package ecommerce.common.config;

import ecommerce.common.util.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AsyncProperties.class)
@RequiredArgsConstructor
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private final AsyncProperties asyncProperties;

    @Bean
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Default Async TaskExecutor initialized");
        return executor;
    }

    private int getInventoryCorePoolSize() { return asyncProperties.getInventory() != null ? asyncProperties.getInventory().getCorePoolSize() : 10; }
    private int getInventoryMaxPoolSize() { return asyncProperties.getInventory() != null ? asyncProperties.getInventory().getMaxPoolSize() : 50; }
    private int getInventoryQueueCapacity() { return asyncProperties.getInventory() != null ? asyncProperties.getInventory().getQueueCapacity() : 500; }

    @Bean("inventoryExecutor")
    public Executor inventoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(getInventoryCorePoolSize());
        executor.setMaxPoolSize(getInventoryMaxPoolSize());
        executor.setQueueCapacity(getInventoryQueueCapacity());
        executor.setThreadNamePrefix("inventory-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Inventory Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getInventory().getCorePoolSize(),
                asyncProperties.getInventory().getMaxPoolSize(),
                asyncProperties.getInventory().getQueueCapacity());
        return executor;
    }

    @Bean("orderExecutor")
    public Executor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getOrder().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getOrder().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getOrder().getQueueCapacity());
        executor.setThreadNamePrefix("order-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Order Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getOrder().getCorePoolSize(),
                asyncProperties.getOrder().getMaxPoolSize(),
                asyncProperties.getOrder().getQueueCapacity());
        return executor;
    }

    @Bean("paymentExecutor")
    public Executor paymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getPayment().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getPayment().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getPayment().getQueueCapacity());
        executor.setThreadNamePrefix("payment-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Payment Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getPayment().getCorePoolSize(),
                asyncProperties.getPayment().getMaxPoolSize(),
                asyncProperties.getPayment().getQueueCapacity());
        return executor;
    }

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getNotification().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getNotification().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getNotification().getQueueCapacity());
        executor.setThreadNamePrefix("notification-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Notification Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getNotification().getCorePoolSize(),
                asyncProperties.getNotification().getMaxPoolSize(),
                asyncProperties.getNotification().getQueueCapacity());
        return executor;
    }

    @Bean("reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getReport().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getReport().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getReport().getQueueCapacity());
        executor.setThreadNamePrefix("report-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Report Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getReport().getCorePoolSize(),
                asyncProperties.getReport().getMaxPoolSize(),
                asyncProperties.getReport().getQueueCapacity());
        return executor;
    }

    @Bean("imageExecutor")
    public Executor imageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getImage().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getImage().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getImage().getQueueCapacity());
        executor.setThreadNamePrefix("image-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Image Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getImage().getCorePoolSize(),
                asyncProperties.getImage().getMaxPoolSize(),
                asyncProperties.getImage().getQueueCapacity());
        return executor;
    }

    @Bean("searchExecutor")
    public Executor searchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getSearch().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getSearch().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getSearch().getQueueCapacity());
        executor.setThreadNamePrefix("search-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Search Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncProperties.getSearch().getCorePoolSize(),
                asyncProperties.getSearch().getMaxPoolSize(),
                asyncProperties.getSearch().getQueueCapacity());
        return executor;
    }

    @Bean("securityEventExecutor")
    public Executor securityEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("security-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    // Analytics events are fire-and-forget after TX commit; small pool is sufficient.
    @Bean("analyticsExecutor")
    public Executor analyticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analytics-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
