--liquibase formatted sql

--changeset fynza:002-seed-data dbms:postgresql

-- Insert sample users (customers)
INSERT INTO users (id, username, email, password, first_name, last_name, phone, profile_image_url, role, status, email_verified, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'johndoe', 'john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John', 'Doe', '+1234567890', 'https://cdn.fynza.com/avatars/user_123.jpg', 'CUSTOMER', 'ACTIVE', true, true, '2023-01-10 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-222222222222', 'emilyjones', 'emily.jones@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Emily', 'Jones', '+1122334455', NULL, 'CUSTOMER', 'ACTIVE', true, true, '2023-06-20 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-333333333333', 'mikebrown', 'mike.brown@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mike', 'Brown', '+1555666777', NULL, 'CUSTOMER', 'ACTIVE', true, true, '2023-08-05 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert sample seller
INSERT INTO users (id, username, email, password, first_name, last_name, phone, profile_image_url, role, status, email_verified, is_active, created_at, updated_at) VALUES
('44444444-4444-4444-4444-444444444444', 'sarahsmith', 'seller@premiumfootwear.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Sarah', 'Smith', '+1987654321', NULL, 'SELLER', 'ACTIVE', true, true, '2023-03-15 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert sample admin
INSERT INTO users (id, username, email, password, first_name, last_name, phone, role, status, email_verified, is_active, created_at, updated_at) VALUES
('55555555-5555-5555-5555-555555555555', 'admin', 'admin@fynza.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'User', NULL, 'ADMIN', 'ACTIVE', true, true, '2023-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert categories
INSERT INTO categories (id, name, slug, description, image, featured, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000201', 'Footwear', 'footwear', 'Shoes, sneakers, sandals, and boots', '/category-footwear.jpg', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-000000000202', 'Electronics', 'electronics', 'Electronic devices and gadgets', '/category-electronics.jpg', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-000000000203', 'Accessories', 'accessories', 'Bags, watches, jewelry and more', '/category-accessories.jpg', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-000000000204', 'Fashion', 'fashion', 'Clothing and fashion items', '/category-fashion.jpg', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Insert products
INSERT INTO products (id, name, slug, brand, sku, description, price, original_price, discount, stock, available_quantity, sold_quantity, rating, reserved_quantity, review_count, view_count, category_id, seller_id, status, is_approved, inventory_status, featured, is_new, is_bestseller, main_image_url, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000001', 'Premium Sneakers', 'premium-sneakers', 'Nike', 'SNK-001', 'High-quality sports sneakers with premium comfort', 149.99, 199.99, 25.00, 45, 45, 156, 4.50, 0, 128, 500, '11111111-1111-1111-1111-000000000201', '44444444-4444-4444-4444-444444444444', 'ACTIVE', true, 'IN_STOCK', true, false, true, 'https://cdn.fynza.com/products/sneakers_main.jpg', true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('11111111-1111-1111-1111-000000000002', 'Wireless Headphones', 'wireless-headphones', 'Sony', 'ELE-003', 'Premium noise-cancelling wireless headphones', 299.99, 399.99, 25.00, 30, 30, 342, 4.70, 0, 342, 800, '22222222-2222-2222-2222-000000000202', '44444444-4444-4444-4444-444444444444', 'ACTIVE', true, 'IN_STOCK', true, false, false, 'https://cdn.fynza.com/products/headphones_main.jpg', true, '2024-01-20 10:00:00', '2024-03-15 10:00:00'),
('11111111-1111-1111-1111-000000000003', 'Sports Watch', 'sports-watch', 'Garmin', 'ACC-004', 'Fitness tracking smartwatch with GPS', 199.99, 249.99, 20.00, 55, 55, 205, 4.40, 0, 205, 450, '33333333-3333-3333-3333-000000000203', '44444444-4444-4444-4444-444444444444', 'ACTIVE', true, 'IN_STOCK', false, false, false, 'https://cdn.fynza.com/products/watch_main.jpg', true, '2024-02-10 10:00:00', '2024-03-15 10:00:00'),
('11111111-1111-1111-1111-000000000004', 'Laptop Backpack', 'laptop-backpack', 'SwissGear', 'ACC-006', 'Durable laptop backpack with multiple compartments', 89.99, 129.99, 30.00, 80, 80, 178, 4.50, 0, 178, 320, '33333333-3333-3333-3333-000000000203', '44444444-4444-4444-4444-444444444444', 'ACTIVE', true, 'IN_STOCK', false, false, true, 'https://cdn.fynza.com/products/backpack_main.jpg', true, '2024-02-05 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert addresses for customers
INSERT INTO addresses (id, user_id, label, street_address, city, state, postal_code, country, address_type, is_default, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000031', '11111111-1111-1111-1111-111111111111', 'Home', '123 Main St', 'New York', 'NY', '10001', 'USA', 'SHIPPING', true, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000032', '11111111-1111-1111-1111-111111111111', 'Office', '456 Business Ave', 'New York', 'NY', '10002', 'USA', 'SHIPPING', false, true, '2024-02-20 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000033', '22222222-2222-2222-2222-222222222222', 'Home', '789 Park Lane', 'Los Angeles', 'CA', '90001', 'USA', 'SHIPPING', true, true, '2024-03-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert wishlists
INSERT INTO wishlists (id, user_id, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000041', '11111111-1111-1111-1111-111111111111', true, '2024-03-10 10:30:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000042', '22222222-2222-2222-2222-222222222222', true, '2024-03-11 14:20:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert wishlist items
INSERT INTO wishlist_items (id, wishlist_id, user_id, product_id, added_at, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000051', '11111111-1111-1111-1111-000000000041', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-000000000001', '2024-03-10 10:30:00', true, '2024-03-10 10:30:00', '2024-03-10 10:30:00'),
('22222222-2222-2222-2222-000000000052', '22222222-2222-2222-2222-000000000042', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-000000000002', '2024-03-11 14:20:00', true, '2024-03-11 14:20:00', '2024-03-11 14:20:00'),
('33333333-3333-3333-3333-000000000053', '22222222-2222-2222-2222-000000000042', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-000000000004', '2024-03-12 09:15:00', true, '2024-03-12 09:15:00', '2024-03-12 09:15:00')
ON CONFLICT (id) DO NOTHING;

-- Insert orders
INSERT INTO orders (id, order_number, customer_id, subtotal, tax, shipping_cost, discount, total_amount, status, payment_status, tracking_number, estimated_delivery, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000011', 'FYZ-2024-001', '11111111-1111-1111-1111-111111111111', 259.99, 20.00, 20.00, 0.00, 299.99, 'DELIVERED', 'PAID', 'FDX123456789', '2024-03-15 10:00:00', true, '2024-03-10 10:30:00', '2024-03-13 16:45:00'),
('22222222-2222-2222-2222-000000000022', 'FYZ-2024-002', '11111111-1111-1111-1111-111111111111', 399.98, 32.00, 18.00, 0.00, 449.98, 'SHIPPED', 'PAID', 'FDX234567890', '2024-03-16 10:00:00', true, '2024-03-12 14:15:00', '2024-03-13 10:00:00'),
('33333333-3333-3333-3333-000000000033', 'FYZ-2024-003', '22222222-2222-2222-2222-222222222222', 179.99, 14.40, 5.60, 0.00, 199.99, 'DELIVERED', 'PAID', 'FDX345678901', '2024-03-12 10:00:00', true, '2024-03-08 09:45:00', '2024-03-11 15:30:00')
ON CONFLICT (id) DO NOTHING;

-- Insert order items
INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, price, subtotal, size, color, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000021', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-000000000001', '44444444-4444-4444-4444-444444444444', 1, 149.99, 149.99, '42', 'Black', true, '2024-03-10 10:30:00', '2024-03-10 10:30:00'),
('22222222-2222-2222-2222-000000000022', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-000000000003', '44444444-4444-4444-4444-444444444444', 1, 199.99, 199.99, NULL, 'Silver', true, '2024-03-10 10:30:00', '2024-03-10 10:30:00'),
('33333333-3333-3333-3333-000000000033', '22222222-2222-2222-2222-000000000022', '11111111-1111-1111-1111-000000000002', '44444444-4444-4444-4444-444444444444', 1, 299.99, 299.99, NULL, 'Black', true, '2024-03-12 14:15:00', '2024-03-12 14:15:00'),
('44444444-4444-4444-4444-000000000044', '33333333-3333-3333-3333-000000000033', '11111111-1111-1111-1111-000000000004', '44444444-4444-4444-4444-444444444444', 1, 89.99, 89.99, NULL, 'Black', true, '2024-03-08 09:45:00', '2024-03-08 09:45:00')
ON CONFLICT (id) DO NOTHING;

-- Insert reviews
INSERT INTO reviews (id, product_id, customer_id, rating, title, comment, verified_purchase, helpful, unhelpful, approved, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000031', '11111111-1111-1111-1111-000000000001', '11111111-1111-1111-1111-111111111111', 5, 'Excellent quality', 'Great shoes, very comfortable and durable. Highly recommended!', true, 45, 2, true, true, '2024-03-10 10:30:00', '2024-03-10 10:30:00'),
('22222222-2222-2222-2222-000000000032', '11111111-1111-1111-1111-000000000001', '22222222-2222-2222-2222-222222222222', 4, 'Good value for money', 'Good product, fits perfectly. Shipping was fast.', true, 32, 1, true, true, '2024-03-09 15:45:00', '2024-03-09 15:45:00'),
('33333333-3333-3333-3333-000000000033', '11111111-1111-1111-1111-000000000002', '33333333-3333-3333-3333-333333333333', 5, 'Best noise cancelling headphones', 'The noise cancellation is phenomenal. Perfect for commuting and travel.', true, 67, 4, true, true, '2024-03-11 14:30:00', '2024-03-11 14:30:00')
ON CONFLICT (id) DO NOTHING;

-- Insert notifications
INSERT INTO notifications (id, user_id, type, title, message, is_read, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000041', '11111111-1111-1111-1111-111111111111', 'ORDER', 'Order Confirmed', 'Your order #FYZ-2024-002 has been confirmed and is being processed.', false, true, '2024-03-12 14:20:00', '2024-03-12 14:20:00'),
('22222222-2222-2222-2222-000000000042', '11111111-1111-1111-1111-111111111111', 'DELIVERY', 'Shipped', 'Your order #FYZ-2024-001 has been shipped and is on its way.', false, true, '2024-03-11 14:00:00', '2024-03-11 14:00:00'),
('33333333-3333-3333-3333-000000000043', '11111111-1111-1111-1111-111111111111', 'PAYMENT', 'Payment Successful', 'Payment of GH₵ 299.99 for order #FYZ-2024-001 was successful.', true, true, '2024-03-10 10:35:00', '2024-03-10 10:35:00'),
('44444444-4444-4444-4444-000000000044', '22222222-2222-2222-2222-222222222222', 'WISHLIST', 'Price Drop Alert', 'Premium Sneakers price has dropped by 15%!', true, true, '2024-03-09 12:00:00', '2024-03-09 12:00:00'),
('55555555-5555-5555-5555-000000000045', '33333333-3333-3333-3333-333333333333', 'PROMOTION', 'Flash Sale', 'Up to 60% off on electronics! Limited time offer.', true, true, '2024-03-08 09:00:00', '2024-03-08 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert seller profile
INSERT INTO seller_profiles (id, user_id, store_name, store_description, store_logo, store_banner, seller_status, verification_status, rating, total_reviews, total_products, total_sales, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000051', '44444444-4444-4444-4444-444444444444', 'BEKIA FASHION', 'Premium fashion store offering the latest trends in footwear and apparel. We specialize in quality shoes, bags, and accessories for the modern consumer.', 'https://cdn.fynza.com/stores/bekia-fashion-logo.jpg', 'https://cdn.fynza.com/stores/bekia-fashion-banner.jpg', 'ACTIVE', 'VERIFIED', 4.70, 328, 24, 2450, true, '2023-03-15 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert carts
INSERT INTO carts (id, user_id, is_active, is_checked_out, is_abandoned, last_activity_at, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000061', '11111111-1111-1111-1111-111111111111', true, false, false, '2024-03-15 10:00:00', '2024-03-15 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000062', '22222222-2222-2222-2222-222222222222', true, false, false, '2024-03-14 15:30:00', '2024-03-14 15:30:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert cart items
INSERT INTO cart_items (id, cart_id, product_id, variant_id, quantity, price, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000071', '11111111-1111-1111-1111-000000000061', '11111111-1111-1111-1111-000000000001', NULL, 1, 149.99, true, '2024-03-15 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000072', '22222222-2222-2222-2222-000000000062', '11111111-1111-1111-1111-000000000002', NULL, 2, 299.99, true, '2024-03-14 15:30:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert order activity logs
INSERT INTO order_activity_logs (id, order_id, user_id, activity_type, description, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000081', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-111111111111', 'ORDER_PLACED', 'Order was placed', true, '2024-03-10 10:30:00', '2024-03-10 10:30:00'),
('22222222-2222-2222-2222-000000000082', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-111111111111', 'ORDER_CONFIRMED', 'Order confirmed', true, '2024-03-10 11:00:00', '2024-03-10 11:00:00'),
('33333333-3333-3333-3333-000000000083', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-111111111111', 'ORDER_SHIPPED', 'Order shipped', true, '2024-03-12 09:00:00', '2024-03-12 09:00:00'),
('44444444-4444-4444-4444-000000000084', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-111111111111', 'ORDER_DELIVERED', 'Order delivered', true, '2024-03-13 16:45:00', '2024-03-13 16:45:00'),
('55555555-5555-5555-5555-000000000085', '22222222-2222-2222-2222-000000000022', '11111111-1111-1111-1111-111111111111', 'ORDER_PLACED', 'Order was placed', true, '2024-03-12 14:15:00', '2024-03-12 14:15:00'),
('66666666-6666-6666-6666-000000000086', '22222222-2222-2222-2222-000000000022', '11111111-1111-1111-1111-111111111111', 'ORDER_SHIPPED', 'Order shipped', true, '2024-03-13 10:00:00', '2024-03-13 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert order timeline
INSERT INTO order_timeline (id, order_id, status, message, timestamp, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000091', '11111111-1111-1111-1111-000000000011', 'PENDING', 'Order placed, awaiting confirmation', '2024-03-10 10:30:00', true, '2024-03-10 10:30:00', '2024-03-10 10:30:00'),
('22222222-2222-2222-2222-000000000092', '11111111-1111-1111-1111-000000000011', 'CONFIRMED', 'Order confirmed by seller', '2024-03-10 11:00:00', true, '2024-03-10 11:00:00', '2024-03-10 11:00:00'),
('33333333-3333-3333-3333-000000000093', '11111111-1111-1111-1111-000000000011', 'SHIPPED', 'Package shipped via FedEx', '2024-03-12 09:00:00', true, '2024-03-12 09:00:00', '2024-03-12 09:00:00'),
('44444444-4444-4444-4444-000000000094', '11111111-1111-1111-1111-000000000011', 'DELIVERED', 'Package delivered', '2024-03-13 16:45:00', true, '2024-03-13 16:45:00', '2024-03-13 16:45:00'),
('55555555-5555-5555-5555-000000000095', '22222222-2222-2222-2222-000000000022', 'PENDING', 'Order placed, awaiting confirmation', '2024-03-12 14:15:00', true, '2024-03-12 14:15:00', '2024-03-12 14:15:00'),
('66666666-6666-6666-6666-000000000096', '22222222-2222-2222-2222-000000000022', 'SHIPPED', 'Package shipped via FedEx', '2024-03-13 10:00:00', true, '2024-03-13 10:00:00', '2024-03-13 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert payment transactions
INSERT INTO payment_transactions (id, order_id, customer_id, amount, currency, payment_method, status, transaction_id, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000101', '11111111-1111-1111-1111-000000000011', '11111111-1111-1111-1111-111111111111', 299.99, 'USD', 'CREDIT_CARD', 'COMPLETED', 'txn_abc123', true, '2024-03-10 10:35:00', '2024-03-10 10:35:00'),
('22222222-2222-2222-2222-000000000102', '22222222-2222-2222-2222-000000000022', '11111111-1111-1111-1111-111111111111', 449.98, 'USD', 'DEBIT_CARD', 'COMPLETED', 'txn_def456', true, '2024-03-12 14:20:00', '2024-03-12 14:20:00'),
('33333333-3333-3333-3333-000000000103', '33333333-3333-3333-3333-000000000033', '22222222-2222-2222-2222-222222222222', 199.99, 'USD', 'PAYPAL', 'COMPLETED', 'txn_ghi789', true, '2024-03-08 09:50:00', '2024-03-08 09:50:00')
ON CONFLICT (id) DO NOTHING;

-- Insert customer profiles
INSERT INTO customer_profiles (id, user_id, loyalty_points, membership_status, total_orders, total_spent, newsletter, preferred_currency, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000111', '11111111-1111-1111-1111-111111111111', 1250, 'SILVER', 2, 499.97, true, 'USD', true, '2023-01-10 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000112', '22222222-2222-2222-2222-222222222222', 450, 'BRONZE', 1, 199.99, false, 'USD', true, '2023-06-20 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert product variants
INSERT INTO product_variants (id, product_id, sku, color, size, price_override, stock, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000121', '11111111-1111-1111-1111-000000000001', 'SNK-001-BLK-42', 'Black', '42', NULL, 10, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000122', '11111111-1111-1111-1111-000000000001', 'SNK-001-BLK-43', 'Black', '43', NULL, 15, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000123', '11111111-1111-1111-1111-000000000001', 'SNK-001-WHT-42', 'White', '42', NULL, 8, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('44444444-4444-4444-4444-000000000124', '11111111-1111-1111-1111-000000000002', 'ELE-003-BLK', 'Black', NULL, 299.99, 30, true, '2024-01-20 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert product images
INSERT INTO product_images (id, product_id, image_url, alt, ordering, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000131', '11111111-1111-1111-1111-000000000001', 'https://cdn.fynza.com/products/sneakers_main.jpg', 'Premium Sneakers Main', 1, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000132', '11111111-1111-1111-1111-000000000001', 'https://cdn.fynza.com/products/sneakers_side.jpg', 'Premium Sneakers Side', 2, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000133', '11111111-1111-1111-1111-000000000001', 'https://cdn.fynza.com/products/sneakers_back.jpg', 'Premium Sneakers Back', 3, true, '2024-01-15 10:00:00', '2024-03-15 10:00:00'),
('44444444-4444-4444-4444-000000000134', '11111111-1111-1111-1111-000000000002', 'https://cdn.fynza.com/products/headphones_main.jpg', 'Wireless Headphones Main', 1, true, '2024-01-20 10:00:00', '2024-03-15 10:00:00'),
('55555555-5555-5555-5555-000000000135', '11111111-1111-1111-1111-000000000002', 'https://cdn.fynza.com/products/headphones_box.jpg', 'Wireless Headphones Box', 2, true, '2024-01-20 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert coupons
INSERT INTO coupons (id, code, description, discount_type, discount_value, min_order_amount, max_uses, usage_count, valid_from, valid_until, status, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000141', 'WELCOME20', '20% off for new customers', 'PERCENTAGE', 20.00, 50.00, 100, 45, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'ACTIVE', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000142', 'FLASH50', '50 GHS off flash sale', 'FIXED', 50.00, 100.00, 50, 12, '2024-03-01 00:00:00', '2024-03-31 23:59:59', 'ACTIVE', true, '2024-03-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert coupon usages
INSERT INTO coupon_usages (id, user_id, coupon_id, order_id, discount_amount, used_at, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000151', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-000000000141', '11111111-1111-1111-1111-000000000011', 60.00, '2024-03-10 10:35:00', true, '2024-03-10 10:35:00', '2024-03-10 10:35:00')
ON CONFLICT (id) DO NOTHING;

-- Insert delivery regions
INSERT INTO delivery_regions (id, name, code, country, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000161', 'Greater Accra', 'GA', 'Ghana', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000162', 'Ashanti', 'AH', 'Ghana', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000163', 'Western', 'WE', 'Ghana', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert delivery fees
INSERT INTO delivery_fees (id, region_id, town_name, base_fee, per_km_fee, estimated_days, delivery_method, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000171', '11111111-1111-1111-1111-000000000161', 'Accra', 10.00, 0.50, 1, 'DIRECT_ADDRESS', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000172', '11111111-1111-1111-1111-000000000161', 'Tema', 15.00, 0.50, 2, 'DIRECT_ADDRESS', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000173', '22222222-2222-2222-2222-000000000162', 'Kumasi', 20.00, 0.80, 3, 'DIRECT_ADDRESS', true, '2024-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert FAQs
INSERT INTO faqs (id, question, answer, category, display_order, view_count, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000181', 'How do I track my order?', 'You can track your order using the tracking number provided in your order confirmation email. Visit our tracking page and enter your order number.', 'ORDERS', 1, 1250, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000182', 'What is your return policy?', 'We offer a 30-day return policy for most items. Items must be unused and in original packaging.', 'RETURNS', 2, 980, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000183', 'How do I become a seller?', 'To become a seller, register for a seller account and complete the verification process. Submit your business documents for review.', 'SELLING', 3, 750, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert site settings
INSERT INTO site_settings (id, site_name, currency, enable_cash_on_delivery, enable_mobile_money, shipping_cost, tax_rate, free_shipping_threshold, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000191', 'Fynza E-commerce', 'GHS', true, true, 10.00, 15.00, 100.00, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert subscribers
INSERT INTO subscribers (id, email, status, subscribed_at, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000192', 'subscriber1@example.com', 'ACTIVE', '2024-02-01 10:00:00', true, '2024-02-01 10:00:00', '2024-02-01 10:00:00'),
('22222222-2222-2222-2222-000000000193', 'subscriber2@example.com', 'ACTIVE', '2024-02-15 10:00:00', true, '2024-02-15 10:00:00', '2024-02-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert tags
INSERT INTO tags (id, name, color, description, is_featured, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000194', 'New Arrivals', '#FF0000', 'New arrival products', true, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000195', 'Best Sellers', '#00FF00', 'Best selling products', true, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00'),
('33333333-3333-3333-3333-000000000196', 'Sale', '#FFA500', 'On sale products', false, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert seller product tags
INSERT INTO seller_product_tags (id, product_id, seller_id, tag_id, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000197', '11111111-1111-1111-1111-000000000001', '44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-000000000195', true, '2024-03-10 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000198', '11111111-1111-1111-1111-000000000002', '44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-000000000194', true, '2024-03-10 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert notification settings
INSERT INTO notification_settings (id, user_id, order_updates, payment_confirmation, shipping_updates, price_drop_alerts, new_product_alerts, promotional_email, promotional_sms, wishlist_updates, review_requests, browser_push, app_push, newsletter, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000199', '11111111-1111-1111-1111-111111111111', true, true, true, true, true, true, false, true, true, true, false, true, true, '2023-01-10 10:00:00', '2024-03-15 10:00:00'),
('22222222-2222-2222-2222-000000000210', '22222222-2222-2222-2222-222222222222', true, true, true, false, false, false, false, true, false, false, false, true, true, '2023-06-20 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert promotions
INSERT INTO promotions (id, name, description, discount_type, discount_value, start_date, end_date, is_featured, is_exclusive, max_uses, current_uses, min_order_amount, max_discount_amount, status, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000211', 'Spring Sale', 'Spring season discount', 'PERCENTAGE', 15.00, '2024-03-01 00:00:00', '2024-03-31 23:59:59', true, false, 200, 85, 50.00, 100.00, 'ACTIVE', true, '2024-03-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert password history
INSERT INTO password_history (id, user_id, password_hash, changed_at, ip_address, is_current, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000212', '11111111-1111-1111-1111-111111111111', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '2023-01-10 10:00:00', '192.168.1.1', true, true, '2023-01-10 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert social links
INSERT INTO social_links (id, facebook_url, twitter_url, instagram_url, linkedin_url, pinterest_url, youtube_url, tiktok_url, whatsapp_number, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000213', 'https://facebook.com/fynza', 'https://twitter.com/fynza', 'https://instagram.com/fynza', NULL, NULL, NULL, NULL, NULL, true, '2024-01-01 10:00:00', '2024-03-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- Insert activity logs
INSERT INTO activity_logs (id, user_id, user_email, user_name, action, activity_type, description, entity_type, entity_id, ip_address, user_agent, request_method, request_path, status, is_active, created_at, updated_at) VALUES
('11111111-1111-1111-1111-000000000214', '11111111-1111-1111-1111-111111111111', 'john.doe@example.com', 'John Doe', 'USER_LOGIN', 'USER_LOGIN', 'User logged in', 'User', '11111111-1111-1111-1111-111111111111', '192.168.1.100', 'Mozilla/5.0', 'POST', '/api/auth/login', 'SUCCESS', true, '2024-03-15 09:00:00', '2024-03-15 09:00:00'),
('22222222-2222-2222-2222-000000000215', '44444444-4444-4444-4444-444444444444', 'seller@premiumfootwear.com', 'Sarah Smith', 'PRODUCT_CREATED', 'PRODUCT_CREATED', 'New product created', 'Product', '11111111-1111-1111-1111-000000000001', '192.168.1.101', 'Mozilla/5.0', 'POST', '/api/seller/products', 'SUCCESS', true, '2024-01-15 10:00:00', '2024-01-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

--rollback DELETE FROM activity_logs WHERE id IN ('11111111-1111-1111-1111-000000000214','22222222-2222-2222-2222-000000000215');
--rollback DELETE FROM social_links WHERE id = '11111111-1111-1111-1111-000000000213';
--rollback DELETE FROM password_history WHERE id = '11111111-1111-1111-1111-000000000212';
--rollback DELETE FROM promotions WHERE id = '11111111-1111-1111-1111-000000000211';
--rollback DELETE FROM notification_settings WHERE id IN ('11111111-1111-1111-1111-000000000199','22222222-2222-2222-2222-000000000210');
--rollback DELETE FROM seller_product_tags WHERE id IN ('11111111-1111-1111-1111-000000000197','22222222-2222-2222-2222-000000000198');
--rollback DELETE FROM tags WHERE id IN ('11111111-1111-1111-1111-000000000194','22222222-2222-2222-2222-000000000195','33333333-3333-3333-3333-000000000196');
--rollback DELETE FROM subscribers WHERE id IN ('11111111-1111-1111-1111-000000000192','22222222-2222-2222-2222-000000000193');
--rollback DELETE FROM site_settings WHERE id = '11111111-1111-1111-1111-000000000191';
--rollback DELETE FROM faqs WHERE id IN ('11111111-1111-1111-1111-000000000181','22222222-2222-2222-2222-000000000182','33333333-3333-3333-3333-000000000183');
--rollback DELETE FROM delivery_fees WHERE id IN ('11111111-1111-1111-1111-000000000171','22222222-2222-2222-2222-000000000172','33333333-3333-3333-3333-000000000173');
--rollback DELETE FROM delivery_regions WHERE id IN ('11111111-1111-1111-1111-000000000161','22222222-2222-2222-2222-000000000162','33333333-3333-3333-3333-000000000163');
--rollback DELETE FROM coupon_usages WHERE id = '11111111-1111-1111-1111-000000000151';
--rollback DELETE FROM coupons WHERE id IN ('11111111-1111-1111-1111-000000000141','22222222-2222-2222-2222-000000000142');
--rollback DELETE FROM product_images WHERE id IN ('11111111-1111-1111-1111-000000000131','22222222-2222-2222-2222-000000000132','33333333-3333-3333-3333-000000000133','44444444-4444-4444-4444-000000000134','55555555-5555-5555-5555-000000000135');
--rollback DELETE FROM product_variants WHERE id IN ('11111111-1111-1111-1111-000000000121','22222222-2222-2222-2222-000000000122','33333333-3333-3333-3333-000000000123','44444444-4444-4444-4444-000000000124');
--rollback DELETE FROM customer_profiles WHERE id IN ('11111111-1111-1111-1111-000000000111','22222222-2222-2222-2222-000000000112');
--rollback DELETE FROM payment_transactions WHERE id IN ('11111111-1111-1111-1111-000000000101','22222222-2222-2222-2222-000000000102','33333333-3333-3333-3333-000000000103');
--rollback DELETE FROM order_timeline WHERE id IN ('11111111-1111-1111-1111-000000000091','22222222-2222-2222-2222-000000000092','33333333-3333-3333-3333-000000000093','44444444-4444-4444-4444-000000000094','55555555-5555-5555-5555-000000000095','66666666-6666-6666-6666-000000000096');
--rollback DELETE FROM order_activity_logs WHERE id IN ('11111111-1111-1111-1111-000000000081','22222222-2222-2222-2222-000000000082','33333333-3333-3333-3333-000000000083','44444444-4444-4444-4444-000000000084','55555555-5555-5555-5555-000000000085','66666666-6666-6666-6666-000000000086');
--rollback DELETE FROM cart_items WHERE id IN ('11111111-1111-1111-1111-000000000071','22222222-2222-2222-2222-000000000072');
--rollback DELETE FROM carts WHERE id IN ('11111111-1111-1111-1111-000000000061','22222222-2222-2222-2222-000000000062');
--rollback DELETE FROM seller_profiles WHERE id = '11111111-1111-1111-1111-000000000051';
--rollback DELETE FROM notifications WHERE id IN ('11111111-1111-1111-1111-000000000041','22222222-2222-2222-2222-000000000042','33333333-3333-3333-3333-000000000043','44444444-4444-4444-4444-000000000044','55555555-5555-5555-5555-000000000045');
--rollback DELETE FROM reviews WHERE id IN ('11111111-1111-1111-1111-000000000031','22222222-2222-2222-2222-000000000032','33333333-3333-3333-3333-000000000033');
--rollback DELETE FROM order_items WHERE id IN ('11111111-1111-1111-1111-000000000021','22222222-2222-2222-2222-000000000022','33333333-3333-3333-3333-000000000033','44444444-4444-4444-4444-000000000044');
--rollback DELETE FROM orders WHERE id IN ('11111111-1111-1111-1111-000000000011','22222222-2222-2222-2222-000000000022','33333333-3333-3333-3333-000000000033');
--rollback DELETE FROM wishlist_items WHERE id IN ('11111111-1111-1111-1111-000000000051','22222222-2222-2222-2222-000000000052','33333333-3333-3333-3333-000000000053');
--rollback DELETE FROM wishlists WHERE id IN ('11111111-1111-1111-1111-000000000041','22222222-2222-2222-2222-000000000042');
--rollback DELETE FROM addresses WHERE id IN ('11111111-1111-1111-1111-000000000031','22222222-2222-2222-2222-000000000032','33333333-3333-3333-3333-000000000033');
--rollback DELETE FROM products WHERE id IN ('11111111-1111-1111-1111-000000000001','11111111-1111-1111-1111-000000000002','11111111-1111-1111-1111-000000000003','11111111-1111-1111-1111-000000000004');
--rollback DELETE FROM categories WHERE id IN ('11111111-1111-1111-1111-000000000201','22222222-2222-2222-2222-000000000202','33333333-3333-3333-3333-000000000203','44444444-4444-4444-4444-000000000204');
--rollback DELETE FROM users WHERE id IN ('11111111-1111-1111-1111-111111111111','22222222-2222-2222-2222-222222222222','33333333-3333-3333-3333-333333333333','44444444-4444-4444-4444-444444444444','55555555-5555-5555-5555-555555555555');
