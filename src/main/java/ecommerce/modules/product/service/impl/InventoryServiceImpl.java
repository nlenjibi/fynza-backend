package ecommerce.modules.product.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;

/**
 * InventoryServiceImpl
 * 
 * Asynchronous implementation using CompletableFuture.
 * Handles stock validation, reservation, and restoration for seller-driven fulfillment.
 * Ensures transactional integrity using ProductRepository.
 * Supports parallel processing for multi-item orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;

    /**
     * Check stock for multiple items in parallel using CompletableFuture.allOf()
     * @param items Map of productId to quantity to check
     * @return CompletableFuture containing a map of productId to boolean (true if stock sufficient)
     */
    @Async("asyncExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Map<UUID, Boolean>> checkStockBatch(Map<UUID, Integer> items) {
        log.debug("Batch checking stock for {} items", items.size());
        
        List<CompletableFuture<Map.Entry<UUID, Boolean>>> futures = items.entrySet().stream()
                .map(entry -> checkStock(entry.getKey(), entry.getValue())
                        .thenApply(result -> Map.entry(entry.getKey(), result)))
                .collect(Collectors.toList());
        
        return allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Reserve stock for multiple items in parallel with rollback capability
     * If any reservation fails, all previously reserved items will be rolled back
     * @param items Map of productId to quantity to reserve
     * @return CompletableFuture containing true if all reservations successful
     */
    @Async("asyncExecutor")
    @Transactional
    public CompletableFuture<Boolean> reserveStockBatch(Map<UUID, Integer> items) {
        log.debug("Batch reserving stock for {} items", items.size());
        
        List<CompletableFuture<Boolean>> futures = items.entrySet().stream()
                .map(entry -> reserveStock(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        
        return allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Collect all results
                    List<Boolean> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    
                    boolean allSuccess = results.stream().allMatch(r -> r);
                    
                    if (!allSuccess) {
                        // Rollback: Restore stock for all items that were successfully reserved
                        log.warn("Batch reservation failed, initiating rollback for {} items", items.size());
                        List<Map.Entry<UUID, Integer>> entries = items.entrySet().stream()
                                .collect(Collectors.toList());
                        for (int i = 0; i < results.size(); i++) {
                            if (results.get(i)) {
                                Map.Entry<UUID, Integer> entry = entries.get(i);
                                try {
                                    restoreStock(entry.getKey(), entry.getValue()).join();
                                    log.info("Rolled back stock for product {}: quantity={}", 
                                            entry.getKey(), entry.getValue());
                                } catch (Exception e) {
                                    log.error("Failed to rollback stock for product {}: {}", 
                                            entry.getKey(), e.getMessage());
                                }
                            }
                        }
                    }
                    
                    return allSuccess;
                });
    }

    @Override
    @Async("asyncExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Boolean> checkStock(UUID productId, Integer quantity) {
        log.debug("Checking stock for product: {}, quantity: {}", productId, quantity);
        
        return CompletableFuture.supplyAsync(() -> {
            Product product = productRepository.findByPublicId(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            
            boolean hasStock = product.getAvailableQuantity() >= quantity;
            log.debug("Stock check result for product {}: available={}, requested={}, sufficient={}",
                    productId, product.getAvailableQuantity(), quantity, hasStock);
            
            return hasStock;
        });
    }

    @Override
    @Async("asyncExecutor")
    @Transactional
    public CompletableFuture<Boolean> reserveStock(UUID productId, Integer quantity) {
        log.debug("Reserving stock for product: {}, quantity: {}", productId, quantity);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Product product = productRepository.findByPublicId(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
                
                if (product.getAvailableQuantity() < quantity) {
                    log.warn("Insufficient stock for product {}: available={}, requested={}",
                            productId, product.getAvailableQuantity(), quantity);
                    return false;
                }
                
                product.reserveStock(quantity);
                productRepository.save(product);
                
                log.info("Stock reserved for product {}: quantity={}, remaining available={}",
                        productId, quantity, product.getAvailableQuantity());
                
                return true;
            } catch (Exception e) {
                log.error("Failed to reserve stock for product {}: {}", productId, e.getMessage(), e);
                throw new RuntimeException("Stock reservation failed", e);
            }
        });
    }

    @Override
    @Async("asyncExecutor")
    @Transactional
    public CompletableFuture<Boolean> reduceStock(UUID productId, Integer quantity) {
        log.debug("Reducing stock for product: {}, quantity: {}", productId, quantity);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Product product = productRepository.findByPublicId(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
                
                product.reduceStock(quantity);
                productRepository.save(product);
                
                log.info("Stock reduced for product {}: quantity={}, remaining stock={}",
                        productId, quantity, product.getStock());
                
                return true;
            } catch (Exception e) {
                log.error("Failed to reduce stock for product {}: {}", productId, e.getMessage(), e);
                throw new RuntimeException("Stock reduction failed", e);
            }
        });
    }

    @Override
    @Async("asyncExecutor")
    @Transactional
    public CompletableFuture<Boolean> restoreStock(UUID productId, Integer quantity) {
        log.debug("Restoring stock for product: {}, quantity: {}", productId, quantity);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Product product = productRepository.findByPublicId(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
                
                product.addStock(quantity);
                productRepository.save(product);
                
                log.info("Stock restored for product {}: quantity={}, new stock={}",
                        productId, quantity, product.getStock());
                
                return true;
            } catch (Exception e) {
                log.error("Failed to restore stock for product {}: {}", productId, e.getMessage(), e);
                throw new RuntimeException("Stock restoration failed", e);
            }
        });
    }
}
