--liquibase formatted sql

--changeset fynza:083-product-search-source-view splitStatements:false
DROP VIEW IF EXISTS v_product_search_source;

CREATE VIEW v_product_search_source AS
SELECT
    p.id                                    AS product_id,
    p.name,
    p.brand,
    p.slug,
    p.sku,
    p.description,
    p.status,
    p.visibility,
    p.seller_id,
    p.store_id,
    p.created_at,
    p.updated_at,
    pc.category_id,
    c.name                                  AS category_name,
    c.slug                                  AS category_slug,
    pr.amount                               AS price,
    pr.currency,
    pr.compare_at_price,
    CASE
        WHEN COALESCE(inv.available_quantity, 0) > 10 THEN 'IN_STOCK'
        WHEN COALESCE(inv.available_quantity, 0) > 0  THEN 'LOW_STOCK'
        ELSE 'OUT_OF_STOCK'
    END                                     AS availability,
    COALESCE(inv.available_quantity, 0)     AS available_quantity,
    rra.average_rating,
    rra.review_count
FROM products p
LEFT JOIN product_categories pc ON pc.product_id = p.id
LEFT JOIN categories c          ON c.id = pc.category_id
LEFT JOIN LATERAL (
    SELECT pr2.amount, pr2.currency, pr2.compare_at_price
    FROM prices pr2
    WHERE pr2.product_id = p.id
      AND pr2.status = 'ACTIVE'
      AND pr2.variant_id IS NULL
    ORDER BY pr2.created_at DESC
    LIMIT 1
) pr ON TRUE
LEFT JOIN LATERAL (
    SELECT i.available_quantity
    FROM inventory i
    WHERE i.product_id = p.id
    ORDER BY i.created_at DESC
    LIMIT 1
) inv ON TRUE
LEFT JOIN review_rating_aggregates rra
    ON rra.target_id = p.id AND rra.target_type = 'PRODUCT';

