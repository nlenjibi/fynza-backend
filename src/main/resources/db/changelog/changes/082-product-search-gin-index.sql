--liquibase formatted sql

--changeset fynza:082-product-search-gin splitStatements:false
CREATE INDEX IF NOT EXISTS idx_products_fts
    ON products
    USING gin(to_tsvector('english',
        coalesce(name, '') || ' ' ||
        coalesce(brand, '') || ' ' ||
        coalesce(description, '')));

