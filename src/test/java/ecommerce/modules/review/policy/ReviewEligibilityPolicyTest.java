package ecommerce.modules.review.policy;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ReviewEligibilityPolicy")
class ReviewEligibilityPolicyTest {

    private ReviewEligibilityPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ReviewEligibilityPolicy();
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private Order orderWithStatus(OrderStatus status) {
        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(status);
        return order;
    }

    private Order orderWithCreatedAt(Instant createdAt) {
        Order order = mock(Order.class);
        when(order.getCreatedAt()).thenReturn(createdAt);
        return order;
    }

    private OrderItem itemWithPublicId(UUID publicId) {
        OrderItem item = mock(OrderItem.class);
        when(item.getPublicId()).thenReturn(publicId);
        return item;
    }

    // ── isOrderEligibleForReview ─────────────────────────────────────────────

    @Nested
    @DisplayName("isOrderEligibleForReview")
    class IsOrderEligibleForReview {

        @Test
        void eligibleStatus_DELIVERED_returnsTrue() {
            Order order = orderWithStatus(OrderStatus.DELIVERED);
            assertThat(policy.isOrderEligibleForReview(order)).isTrue();
        }

        @Test
        void eligibleStatus_CONFIRMED_returnsTrue() {
            Order order = orderWithStatus(OrderStatus.CONFIRMED);
            assertThat(policy.isOrderEligibleForReview(order)).isTrue();
        }

        @Test
        void eligibleStatus_RETURNED_returnsTrue() {
            Order order = orderWithStatus(OrderStatus.RETURNED);
            assertThat(policy.isOrderEligibleForReview(order)).isTrue();
        }

        @Test
        void eligibleStatus_REFUNDED_returnsTrue() {
            Order order = orderWithStatus(OrderStatus.REFUNDED);
            assertThat(policy.isOrderEligibleForReview(order)).isTrue();
        }

        @Test
        void ineligibleStatus_PENDING_returnsFalse() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            assertThat(policy.isOrderEligibleForReview(order)).isFalse();
        }

        @Test
        void ineligibleStatus_CANCELLED_returnsFalse() {
            Order order = orderWithStatus(OrderStatus.CANCELLED);
            assertThat(policy.isOrderEligibleForReview(order)).isFalse();
        }

        @Test
        void nullOrder_returnsFalse() {
            assertThat(policy.isOrderEligibleForReview(null)).isFalse();
        }
    }

    // ── isWithinReviewWindow ─────────────────────────────────────────────────

    @Nested
    @DisplayName("isWithinReviewWindow")
    class IsWithinReviewWindow {

        @Test
        void recentOrder_withinWindow_returnsTrue() {
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            Order order = orderWithCreatedAt(thirtyDaysAgo);
            assertThat(policy.isWithinReviewWindow(order)).isTrue();
        }

        @Test
        void oldOrder_outsideWindow_returnsFalse() {
            Instant hundredDaysAgo = Instant.now().minus(100, ChronoUnit.DAYS);
            Order order = orderWithCreatedAt(hundredDaysAgo);
            assertThat(policy.isWithinReviewWindow(order)).isFalse();
        }

        @Test
        void nullCreatedAt_returnsTrue() {
            Order order = orderWithCreatedAt(null);
            assertThat(policy.isWithinReviewWindow(order)).isTrue();
        }
    }

    // ── isOwner ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isOwner")
    class IsOwner {

        @Test
        void sameCustomerId_returnsTrue() {
            UUID customerId = UUID.randomUUID();
            Order order = mock(Order.class);
            when(order.getCustomerId()).thenReturn(customerId);
            assertThat(policy.isOwner(order, customerId)).isTrue();
        }

        @Test
        void differentCustomerId_returnsFalse() {
            Order order = mock(Order.class);
            when(order.getCustomerId()).thenReturn(UUID.randomUUID());
            assertThat(policy.isOwner(order, UUID.randomUUID())).isFalse();
        }
    }

    // ── orderContainsItem ────────────────────────────────────────────────────

    @Nested
    @DisplayName("orderContainsItem")
    class OrderContainsItem {

        @Test
        void itemPresent_returnsTrue() {
            UUID itemPublicId = UUID.randomUUID();
            OrderItem item = itemWithPublicId(itemPublicId);
            Order order = mock(Order.class);
            when(order.getOrderItems()).thenReturn(List.of(item));
            assertThat(policy.orderContainsItem(order, itemPublicId)).isTrue();
        }

        @Test
        void itemAbsent_returnsFalse() {
            OrderItem item = itemWithPublicId(UUID.randomUUID());
            Order order = mock(Order.class);
            when(order.getOrderItems()).thenReturn(List.of(item));
            assertThat(policy.orderContainsItem(order, UUID.randomUUID())).isFalse();
        }
    }
}
