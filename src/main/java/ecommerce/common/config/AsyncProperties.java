package ecommerce.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {

    private InventoryProperties inventory = new InventoryProperties();
    private OrderProperties order = new OrderProperties();
    private PaymentProperties payment = new PaymentProperties();
    private NotificationProperties notification = new NotificationProperties();
    private ReportProperties report = new ReportProperties();
    private ImageProperties image = new ImageProperties();
    private SearchProperties search = new SearchProperties();
    private TimeoutsProperties timeouts = new TimeoutsProperties();

    public InventoryProperties getInventory() { return inventory; }
    public OrderProperties getOrder() { return order; }
    public PaymentProperties getPayment() { return payment; }
    public NotificationProperties getNotification() { return notification; }
    public ReportProperties getReport() { return report; }
    public ImageProperties getImage() { return image; }
    public SearchProperties getSearch() { return search; }
    public TimeoutsProperties getTimeouts() { return timeouts; }

    public void setInventory(InventoryProperties inventory) { this.inventory = inventory; }
    public void setOrder(OrderProperties order) { this.order = order; }
    public void setPayment(PaymentProperties payment) { this.payment = payment; }
    public void setNotification(NotificationProperties notification) { this.notification = notification; }
    public void setReport(ReportProperties report) { this.report = report; }
    public void setImage(ImageProperties image) { this.image = image; }
    public void setSearch(SearchProperties search) { this.search = search; }
    public void setTimeouts(TimeoutsProperties timeouts) { this.timeouts = timeouts; }

    public static class InventoryProperties {
        @Min(1) @Max(100)
        private int corePoolSize = 10;
        @Min(10) @Max(200)
        private int maxPoolSize = 50;
        @Min(100)
        private int queueCapacity = 500;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class OrderProperties {
        @Min(1) @Max(200)
        private int corePoolSize = 20;
        @Min(10) @Max(500)
        private int maxPoolSize = 100;
        @Min(100)
        private int queueCapacity = 1000;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class PaymentProperties {
        @Min(1) @Max(100)
        private int corePoolSize = 15;
        @Min(10) @Max(200)
        private int maxPoolSize = 75;
        @Min(100)
        private int queueCapacity = 750;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class NotificationProperties {
        @Min(1) @Max(100)
        private int corePoolSize = 10;
        @Min(10) @Max(100)
        private int maxPoolSize = 30;
        @Min(100)
        private int queueCapacity = 500;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class ReportProperties {
        @Min(1) @Max(50)
        private int corePoolSize = 5;
        @Min(5) @Max(100)
        private int maxPoolSize = 20;
        @Min(50)
        private int queueCapacity = 200;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class ImageProperties {
        @Min(1) @Max(50)
        private int corePoolSize = 8;
        @Min(5) @Max(100)
        private int maxPoolSize = 25;
        @Min(50)
        private int queueCapacity = 300;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class SearchProperties {
        @Min(1) @Max(100)
        private int corePoolSize = 10;
        @Min(10) @Max(200)
        private int maxPoolSize = 40;
        @Min(50)
        private int queueCapacity = 400;

        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class TimeoutsProperties {
        private long inventory = 3000;
        private long orderValidation = 5000;
        private long payment = 30000;
        private long notification = 10000;
        private long report = 300000;
        private long image = 60000;
        private long searchIndex = 10000;

        public long getInventory() { return inventory; }
        public long getOrderValidation() { return orderValidation; }
        public long getPayment() { return payment; }
        public long getNotification() { return notification; }
        public long getReport() { return report; }
        public long getImage() { return image; }
        public long getSearchIndex() { return searchIndex; }
        public void setInventory(long inventory) { this.inventory = inventory; }
        public void setOrderValidation(long orderValidation) { this.orderValidation = orderValidation; }
        public void setPayment(long payment) { this.payment = payment; }
        public void setNotification(long notification) { this.notification = notification; }
        public void setReport(long report) { this.report = report; }
        public void setImage(long image) { this.image = image; }
        public void setSearchIndex(long searchIndex) { this.searchIndex = searchIndex; }
    }
}
