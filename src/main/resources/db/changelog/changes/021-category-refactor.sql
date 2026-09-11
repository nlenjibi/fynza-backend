--liquibase formatted sql

--changeset fynza:021-01 labels:category
CREATE TABLE taxonomies (
    id          BIGSERIAL PRIMARY KEY,
    public_id   UUID        NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT,
    status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_taxonomies_code   ON taxonomies (code);
CREATE INDEX idx_taxonomies_status ON taxonomies (status);

-- Seed the default Fynza marketplace taxonomy
INSERT INTO taxonomies (public_id, name, code, description, status)
VALUES (gen_random_uuid(), 'Fynza Marketplace', 'fynza-marketplace', 'Default Fynza product taxonomy', 'ACTIVE');

--changeset fynza:021-02 labels:category
-- Add new columns to categories
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS public_id    UUID         DEFAULT gen_random_uuid() UNIQUE,
    ADD COLUMN IF NOT EXISTS taxonomy_id  BIGINT       REFERENCES taxonomies(id),
    ADD COLUMN IF NOT EXISTS status       VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS visibility   VARCHAR(30)  NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN IF NOT EXISTS sort_order   INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS media_id     VARCHAR(255);

-- Ensure public_id is populated for any existing rows (back-fill)
UPDATE categories SET public_id = gen_random_uuid() WHERE public_id IS NULL;

-- Back-fill taxonomy_id for existing rows
UPDATE categories SET taxonomy_id = (SELECT id FROM taxonomies WHERE code = 'fynza-marketplace' LIMIT 1)
WHERE taxonomy_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_categories_taxonomy_id ON categories (taxonomy_id);
CREATE INDEX IF NOT EXISTS idx_categories_status      ON categories (status);
CREATE INDEX IF NOT EXISTS idx_categories_visibility  ON categories (visibility);
CREATE INDEX IF NOT EXISTS idx_categories_sort_order  ON categories (sort_order);

--changeset fynza:021-03 labels:category
CREATE TABLE category_status_history (
    id               BIGSERIAL    PRIMARY KEY,
    public_id        UUID         NOT NULL UNIQUE,
    category_id      UUID         NOT NULL REFERENCES categories(id),
    previous_status  VARCHAR(30),
    new_status       VARCHAR(30)  NOT NULL,
    reason           TEXT,
    changed_by       UUID,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_cat_status_hist_category  ON category_status_history (category_id);
CREATE INDEX idx_cat_status_hist_created   ON category_status_history (created_at);

--changeset fynza:021-04 labels:category
CREATE TABLE attribute_definitions (
    id                BIGSERIAL    PRIMARY KEY,
    public_id         UUID         NOT NULL UNIQUE,
    category_id       UUID         NOT NULL REFERENCES categories(id),
    name              VARCHAR(100) NOT NULL,
    code              VARCHAR(100) NOT NULL,
    data_type         VARCHAR(30)  NOT NULL,
    unit              VARCHAR(50),
    required          BOOLEAN      NOT NULL DEFAULT FALSE,
    filterable        BOOLEAN      NOT NULL DEFAULT FALSE,
    searchable        BOOLEAN      NOT NULL DEFAULT FALSE,
    variant_defining  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order        INT          NOT NULL DEFAULT 0,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (category_id, code)
);

CREATE INDEX idx_attr_def_category   ON attribute_definitions (category_id);
CREATE INDEX idx_attr_def_code       ON attribute_definitions (code);
CREATE INDEX idx_attr_def_filterable ON attribute_definitions (filterable);

--changeset fynza:021-05 labels:category
CREATE TABLE attribute_options (
    id                      BIGSERIAL    PRIMARY KEY,
    public_id               UUID         NOT NULL UNIQUE,
    attribute_definition_id BIGINT       NOT NULL REFERENCES attribute_definitions(id),
    value                   VARCHAR(255) NOT NULL,
    label                   VARCHAR(255) NOT NULL,
    sort_order              INT          NOT NULL DEFAULT 0,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_attr_opts_definition ON attribute_options (attribute_definition_id);

--changeset fynza:021-06 labels:category
CREATE TABLE category_suggestions (
    id                 BIGSERIAL    PRIMARY KEY,
    public_id          UUID         NOT NULL UNIQUE,
    requested_by       UUID         NOT NULL,
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    parent_category_id UUID         REFERENCES categories(id),
    reason             TEXT,
    status             VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    reviewed_by        UUID,
    reviewed_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_cat_suggestions_status       ON category_suggestions (status);
CREATE INDEX idx_cat_suggestions_requested_by ON category_suggestions (requested_by);

--changeset fynza:021-07 labels:category
CREATE OR REPLACE VIEW v_category_summary AS
SELECT
    c.id,
    c.public_id,
    c.taxonomy_id,
    c.parent_category_id  AS parent_id,
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
