--liquibase formatted sql
--changeset fynza:018-products-refactor dbms:postgresql

-- Drop FK constraint on old seller_id (UUID → users)
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_seller_id_fkey;
ALTER TABLE products DROP COLUMN IF EXISTS seller_id;

-- Drop FK constraint on category_id (replaced by product_categories junction table)
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_id_fkey;
ALTER TABLE products DROP COLUMN IF EXISTS category_id;

-- Drop pricing-domain columns
ALTER TABLE products DROP COLUMN IF EXISTS price;
ALTER TABLE products DROP COLUMN IF EXISTS original_price;
ALTER TABLE products DROP COLUMN IF EXISTS discount;

-- Drop inventory-domain columns
ALTER TABLE products DROP COLUMN IF EXISTS stock;
ALTER TABLE products DROP COLUMN IF EXISTS available_quantity;
ALTER TABLE products DROP COLUMN IF EXISTS sold_quantity;
ALTER TABLE products DROP COLUMN IF EXISTS reserved_quantity;
ALTER TABLE products DROP COLUMN IF EXISTS inventory_status;

-- Drop analytics / review-domain columns (derived values, owned by other modules)
ALTER TABLE products DROP COLUMN IF EXISTS rating;
ALTER TABLE products DROP COLUMN IF EXISTS review_count;
ALTER TABLE products DROP COLUMN IF EXISTS view_count;

-- Drop merchandising flags (replaced by visibility + status lifecycle)
ALTER TABLE products DROP COLUMN IF EXISTS featured;
ALTER TABLE products DROP COLUMN IF EXISTS is_new;
ALTER TABLE products DROP COLUMN IF EXISTS is_bestseller;
ALTER TABLE products DROP COLUMN IF EXISTS is_approved;

-- Drop legacy media URL (replaced by product_media table)
ALTER TABLE products DROP COLUMN IF EXISTS main_image_url;

-- Add catalog-identity columns
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_number VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS store_id      BIGINT REFERENCES stores(id);
ALTER TABLE products ADD COLUMN IF NOT EXISTS seller_id     BIGINT REFERENCES sellers(id);
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type  VARCHAR(30) NOT NULL DEFAULT 'SIMPLE';
ALTER TABLE products ADD COLUMN IF NOT EXISTS visibility    VARCHAR(30) NOT NULL DEFAULT 'PUBLIC';

-- Unique constraint on product_number (NULLs are permitted until number is assigned)
ALTER TABLE products ADD CONSTRAINT uk_product_number UNIQUE (product_number);

-- Change status default from PENDING → DRAFT
ALTER TABLE products ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Indexes
CREATE INDEX IF NOT EXISTS idx_products_product_number ON products(product_number);
CREATE INDEX IF NOT EXISTS idx_products_store_id       ON products(store_id);
CREATE INDEX IF NOT EXISTS idx_products_seller_id      ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_products_visibility     ON products(visibility);
CREATE INDEX IF NOT EXISTS idx_products_created_at     ON products(created_at);

-- ── product_variants refactor ────────────────────────────────────────────────

-- Drop pricing-domain column
ALTER TABLE product_variants DROP COLUMN IF EXISTS price_override;

-- Drop inventory-domain column
ALTER TABLE product_variants DROP COLUMN IF EXISTS stock;

-- Add new catalog columns
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS barcode        VARCHAR(100);
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS variant_name   VARCHAR(255);
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS variant_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

-- Indexes
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_status     ON product_variants(variant_status);

--rollback SELECT 'rollback not supported for 018-products-refactor';
