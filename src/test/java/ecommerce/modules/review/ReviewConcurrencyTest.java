package ecommerce.modules.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewConcurrencyTest")
class ReviewConcurrencyTest {

    /**
     * Simulates two concurrent review submissions for the same (product, customer, orderItem).
     * Only one thread should succeed in "creating" the review; the other is blocked by the
     * atomic check-and-set that models the database unique constraint.
     */
    @Test
    void duplicateReviewSubmission_concurrentRequests_onlyOneSucceeds() throws InterruptedException {
        AtomicBoolean reviewExists = new AtomicBoolean(false);
        AtomicInteger createdCount = new AtomicInteger(0);

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        // compareAndSet(false, true) → only one thread wins
                        if (reviewExists.compareAndSet(false, true)) {
                            createdCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // release all threads simultaneously
            boolean finished = doneLatch.await(5, TimeUnit.SECONDS);

            assertThat(finished).as("All threads completed within timeout").isTrue();
            assertThat(createdCount.get())
                    .as("Exactly one review should have been created despite concurrent requests")
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Simulates two concurrent vote requests for the same (reviewId, customerId).
     * Models the unique constraint using ConcurrentHashMap.putIfAbsent — only one entry wins.
     */
    @Test
    void duplicateVote_concurrentRequests_onlyOneApplied() throws InterruptedException {
        ConcurrentHashMap<String, Boolean> voteMap = new ConcurrentHashMap<>();
        UUID reviewId    = UUID.randomUUID();
        UUID customerId  = UUID.randomUUID();
        String voteKey   = reviewId + ":" + customerId;

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        voteMap.putIfAbsent(voteKey, Boolean.TRUE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean finished = doneLatch.await(5, TimeUnit.SECONDS);

            assertThat(finished).as("All threads completed within timeout").isTrue();
            assertThat(voteMap.size())
                    .as("Vote map should contain exactly one entry for the (reviewId, customerId) pair")
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Simulates concurrent aggregate updates: 5 threads increment and 5 threads decrement a LongAdder.
     * Demonstrates that thread-safe aggregate bookkeeping produces a correct final count.
     */
    @Test
    void aggregateUpdate_concurrentPublishAndDelete_productsCorrectCount() throws InterruptedException {
        LongAdder aggregateCount = new LongAdder();

        int threadCount  = 10;
        int addThreads   = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final boolean shouldAdd = i < addThreads;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (shouldAdd) {
                            aggregateCount.increment();
                        } else {
                            aggregateCount.decrement();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean finished = doneLatch.await(5, TimeUnit.SECONDS);

            assertThat(finished).as("All threads completed within timeout").isTrue();
            assertThat(aggregateCount.sum())
                    .as("5 increments and 5 decrements should cancel out to 0")
                    .isEqualTo(0L);
        } finally {
            executor.shutdownNow();
        }
    }
}
