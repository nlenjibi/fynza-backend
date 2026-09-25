--liquibase formatted sql

--changeset fynza:076-category-bigserial-id splitStatements:false
ALTER TABLE categories ADD COLUMN IF NOT EXISTS internal_id BIGSERIAL;

DROP VIEW IF EXISTS v_category_summary;

CREATE VIEW v_category_summary AS
SELECT
    c.internal_id                AS id,
    c.id                         AS category_uuid,
    c.public_id,
    c.taxonomy_id,
    parent_cat.internal_id       AS parent_id,
    c.name,
    c.slug,
    c.description,
    c.status,
    c.visibility,
    c.sort_order,
    c.media_id,
    c.is_active,
    c.created_at,
    c.updated_at,
    COALESCE(pc.product_count,        0) AS product_count,
    COALESCE(pc.active_product_count, 0) AS active_product_count,
    COALESCE(cc.child_count,          0) AS child_category_count
FROM categories c
LEFT JOIN categories parent_cat ON parent_cat.id = c.parent_category_id
LEFT JOIN (
    SELECT category_id,
           COUNT(*)                                           AS product_count,
           COUNT(*) FILTER (WHERE p.status = 'ACTIVE')       AS active_product_count
    FROM product_categories pc2
    JOIN products p ON p.id = pc2.product_id
    GROUP BY category_id
) pc ON pc.category_id = c.id
LEFT JOIN (
    SELECT parent_category_id, COUNT(*) AS child_count
    FROM categories
    WHERE parent_category_id IS NOT NULL
    GROUP BY parent_category_id
) cc ON cc.parent_category_id = c.id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_internal_id ON categories (internal_id);
