package ecommerce.common.event.inventory;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

/**
 * Published when a product's available quantity drops below a threshold after a sale.
 * Consumed to: alert the seller via notification/email so they can restock.
 */
public record LowStockEvent(
        Long productId,
        UUID publicProductId,
        String productName,
        String sku,
        int currentStock,
        int threshold,
        Long sellerId,
        String sellerEmail,
        String sellerName
) implements DomainEvent {}
