--liquibase formatted sql

--changeset fynza:001-initial-schema dbms:postgresql
-- Complete initial schema for Fynza E-commerce (all 63 tables, FK dependency order)

-- ============================================================
-- Group 1: No foreign key dependencies
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id                    UUID         PRIMARY KEY,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    username              VARCHAR(100) NOT NULL UNIQUE,
    password              TEXT         NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    phone                 VARCHAR(20),
    profile_image_url     VARCHAR(500),
    date_of_birth         TIMESTAMP,
    role                  VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    status                VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    email_verified        BOOLEAN      DEFAULT FALSE,
    is_locked             BOOLEAN      DEFAULT FALSE,
    last_login_at         TIMESTAMP,
    last_password_change  TIMESTAMP,
    google_id             VARCHAR(255),
    github_id             VARCHAR(255),
    oauth_provider        VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role     ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status   ON users(status);

CREATE TABLE IF NOT EXISTS categories (
    id                 UUID         PRIMARY KEY,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    name               VARCHAR(100) NOT NULL UNIQUE,
    description        TEXT,
    parent_category_id UUID         REFERENCES categories(id),
    image              VARCHAR(500),
    slug               VARCHAR(150) NOT NULL UNIQUE,
    featured           BOOLEAN      DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_category_slug      ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_category_parent_id ON categories(parent_category_id);
CREATE INDEX IF NOT EXISTS idx_category_name      ON categories(name);
CREATE INDEX IF NOT EXISTS idx_category_is_active ON categories(is_active);

CREATE TABLE IF NOT EXISTS delivery_regions (
    id         UUID         PRIMARY KEY,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(10)  NOT NULL UNIQUE,
    country    VARCHAR(100) NOT NULL DEFAULT 'Ghana'
);
CREATE INDEX IF NOT EXISTS idx_region_name ON delivery_regions(name);

CREATE TABLE IF NOT EXISTS tags (
    id          UUID         PRIMARY KEY,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    color       VARCHAR(20),
    icon        VARCHAR(50),
    is_featured BOOLEAN      DEFAULT FALSE,
    usage_count INTEGER      DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tag_active ON tags(is_active);

CREATE TABLE IF NOT EXISTS site_settings (
    id                          UUID          PRIMARY KEY,
    is_active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP     NOT NULL,
    updated_at                  TIMESTAMP     NOT NULL,
    site_name                   VARCHAR(255),
    site_email                  VARCHAR(255),
    site_phone                  VARCHAR(50),
    currency                    VARCHAR(10)   DEFAULT 'USD',
    timezone                    VARCHAR(50)   DEFAULT 'America/New_York',
    tax_rate                    DECIMAL(5,2)  DEFAULT 0,
    shipping_cost               DECIMAL(10,2) DEFAULT 0,
    free_shipping_threshold     DECIMAL(10,2) DEFAULT 0,
    paystack_public_key         VARCHAR(500),
    paystack_secret_key         VARCHAR(500),
    enable_cash_on_delivery     BOOLEAN       DEFAULT TRUE,
    enable_mobile_money         BOOLEAN       DEFAULT TRUE,
    estimated_delivery_days     VARCHAR(20),
    tax_number                  VARCHAR(50),
    smtp_host                   VARCHAR(255),
    smtp_port                   VARCHAR(10),
    smtp_email                  VARCHAR(255),
    smtp_password               VARCHAR(500),
    email_notifications         BOOLEAN       DEFAULT TRUE,
    order_notifications         BOOLEAN       DEFAULT TRUE,
    refund_notifications        BOOLEAN       DEFAULT TRUE,
    seller_notifications        BOOLEAN       DEFAULT TRUE,
    two_factor_enabled          BOOLEAN       DEFAULT TRUE,
    session_timeout_minutes     INTEGER       DEFAULT 30,
    login_notifications         BOOLEAN       DEFAULT TRUE,
    enable_order_updates        BOOLEAN       DEFAULT TRUE,
    enable_payment_confirmation BOOLEAN       DEFAULT TRUE,
    enable_shipping_updates     BOOLEAN       DEFAULT TRUE,
    enable_promotional_emails   BOOLEAN       DEFAULT TRUE,
    enable_new_product_alerts   BOOLEAN       DEFAULT TRUE,
    enable_price_drop_alerts    BOOLEAN       DEFAULT TRUE,
    enable_wishlist_updates     BOOLEAN       DEFAULT TRUE,
    enable_review_requests      BOOLEAN       DEFAULT TRUE,
    enable_newsletter           BOOLEAN       DEFAULT TRUE,
    enable_promotional_sms      BOOLEAN       DEFAULT FALSE,
    enable_browser_notifications BOOLEAN      DEFAULT TRUE,
    enable_app_notifications    BOOLEAN       DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS social_links (
    id               UUID         PRIMARY KEY,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    facebook_url     VARCHAR(500),
    twitter_url      VARCHAR(500),
    instagram_url    VARCHAR(500),
    linkedin_url     VARCHAR(500),
    youtube_url      VARCHAR(500),
    tiktok_url       VARCHAR(500),
    pinterest_url    VARCHAR(500),
    whatsapp_number  VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS coupons (
    id               UUID          PRIMARY KEY,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    code             VARCHAR(50)   NOT NULL UNIQUE,
    description      TEXT,
    discount_type    VARCHAR(20)   NOT NULL,
    discount_value   DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2),
    max_uses         INTEGER,
    usage_count      INTEGER       NOT NULL DEFAULT 0,
    valid_from       TIMESTAMP     NOT NULL,
    valid_until      TIMESTAMP     NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
);
CREATE INDEX IF NOT EXISTS idx_coupon_status ON coupons(status);

CREATE TABLE IF NOT EXISTS subscribers (
    id               UUID         PRIMARY KEY,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    subscribed_at    TIMESTAMP    NOT NULL,
    unsubscribed_at  TIMESTAMP,
    ip_address       VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_subscriber_status  ON subscribers(status);
CREATE INDEX IF NOT EXISTS idx_subscriber_created ON subscribers(created_at);

CREATE TABLE IF NOT EXISTS faqs (
    id            UUID         PRIMARY KEY,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    question      VARCHAR(500) NOT NULL,
    answer        TEXT         NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    view_count    INTEGER      NOT NULL DEFAULT 0,
    display_order INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_faq_category ON faqs(category);
CREATE INDEX IF NOT EXISTS idx_faq_active   ON faqs(is_active);

CREATE TABLE IF NOT EXISTS contact_messages (
    id            UUID         PRIMARY KEY,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(50),
    subject       VARCHAR(255) NOT NULL,
    message       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    priority      VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    category      VARCHAR(30)  NOT NULL DEFAULT 'GENERAL_INQUIRY',
    assigned_to   UUID,
    admin_response TEXT,
    responded_at  TIMESTAMP,
    responded_by  VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_contact_status   ON contact_messages(status);
CREATE INDEX IF NOT EXISTS idx_contact_priority ON contact_messages(priority);
CREATE INDEX IF NOT EXISTS idx_contact_category ON contact_messages(category);
CREATE INDEX IF NOT EXISTS idx_contact_created  ON contact_messages(created_at);

CREATE TABLE IF NOT EXISTS search_analytics (
    id                   UUID         PRIMARY KEY,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    search_query         VARCHAR(500) NOT NULL,
    search_date          DATE         NOT NULL,
    search_count         INTEGER      NOT NULL DEFAULT 0,
    result_count         INTEGER      NOT NULL DEFAULT 0,
    click_count          INTEGER      NOT NULL DEFAULT 0,
    user_id              UUID,
    session_id           VARCHAR(100),
    search_type          VARCHAR(50),
    ip_address           VARCHAR(50),
    avg_response_time_ms BIGINT,
    is_zero_results      BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_search_query ON search_analytics(search_query);
CREATE INDEX IF NOT EXISTS idx_search_date  ON search_analytics(search_date);

CREATE TABLE IF NOT EXISTS admin_flash_sales (
    id                     UUID          PRIMARY KEY,
    is_active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP     NOT NULL,
    updated_at             TIMESTAMP     NOT NULL,
    name                   VARCHAR(200)  NOT NULL,
    description            TEXT,
    discount_percent       INTEGER       NOT NULL,
    min_purchase_amount    DECIMAL(10,2),
    max_discount_amount    DECIMAL(10,2),
    max_products_per_seller INTEGER      DEFAULT 10,
    max_total_products     INTEGER       DEFAULT 100,
    current_products_count INTEGER       DEFAULT 0,
    category               VARCHAR(100),
    banner_image           VARCHAR(500),
    start_datetime         TIMESTAMP     NOT NULL,
    end_datetime           TIMESTAMP     NOT NULL,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    created_by             UUID
);
CREATE INDEX IF NOT EXISTS idx_flash_sale_status ON admin_flash_sales(status);
CREATE INDEX IF NOT EXISTS idx_flash_sale_dates  ON admin_flash_sales(start_datetime, end_datetime);

CREATE TABLE IF NOT EXISTS admin_promotions (
    id                 UUID          PRIMARY KEY,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    name               VARCHAR(255)  NOT NULL,
    promotion_type     VARCHAR(30)   NOT NULL,
    code               VARCHAR(50),
    discount_value     DECIMAL(10,2) NOT NULL,
    discount_type      VARCHAR(20)   DEFAULT 'PERCENTAGE',
    min_purchase       DECIMAL(10,2),
    max_discount       DECIMAL(10,2),
    start_date         TIMESTAMP     NOT NULL,
    end_date           TIMESTAMP     NOT NULL,
    usage_limit        INTEGER,
    usage_count        INTEGER       NOT NULL DEFAULT 0,
    total_revenue      DECIMAL(15,2) DEFAULT 0,
    status             VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    is_global          BOOLEAN       NOT NULL DEFAULT FALSE,
    target_category_id UUID
);
CREATE INDEX IF NOT EXISTS idx_admin_promo_status ON admin_promotions(status);
CREATE INDEX IF NOT EXISTS idx_admin_promo_type   ON admin_promotions(promotion_type);

CREATE TABLE IF NOT EXISTS promotions (
    id                  UUID          PRIMARY KEY,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    name                VARCHAR(255)  NOT NULL,
    description         TEXT,
    banner_image        VARCHAR(500),
    discount_type       VARCHAR(50),
    discount_value      DECIMAL(10,2),
    min_order_amount    DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    start_date          TIMESTAMP,
    end_date            TIMESTAMP,
    max_uses            INTEGER,
    current_uses        INTEGER       DEFAULT 0,
    max_uses_per_user   INTEGER       DEFAULT 1,
    is_exclusive        BOOLEAN       DEFAULT FALSE,
    is_featured         BOOLEAN       DEFAULT FALSE,
    terms_conditions    TEXT,
    status              VARCHAR(20)   DEFAULT 'ACTIVE'
);
CREATE INDEX IF NOT EXISTS idx_promotion_active ON promotions(is_active);
CREATE INDEX IF NOT EXISTS idx_promotion_dates  ON promotions(start_date, end_date);

CREATE TABLE IF NOT EXISTS reports (
    id            UUID         PRIMARY KEY,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    report_number VARCHAR(50)  NOT NULL UNIQUE,
    report_type   VARCHAR(30)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    format        VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    file_path     VARCHAR(255),
    file_size     BIGINT,
    start_date    TIMESTAMP,
    end_date      TIMESTAMP,
    created_by    UUID         NOT NULL,
    completed_at  TIMESTAMP,
    error_message TEXT,
    filters       TEXT
);
CREATE INDEX IF NOT EXISTS idx_report_type       ON reports(report_type);
CREATE INDEX IF NOT EXISTS idx_report_status     ON reports(status);
CREATE INDEX IF NOT EXISTS idx_report_created_by ON reports(created_by);

CREATE TABLE IF NOT EXISTS report_schedules (
    id             UUID         PRIMARY KEY,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    schedule_name  VARCHAR(255) NOT NULL,
    report_type    VARCHAR(30)  NOT NULL,
    schedule_type  VARCHAR(20)  NOT NULL,
    format         VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    cron_expression VARCHAR(255),
    day_of_week    VARCHAR(20),
    day_of_month   INTEGER,
    hour           INTEGER,
    minute         INTEGER,
    start_date     TIMESTAMP,
    end_date       TIMESTAMP,
    last_run_at    TIMESTAMP,
    next_run_at    TIMESTAMP,
    recipients     TEXT,
    created_by     UUID         NOT NULL,
    filters        TEXT
);
CREATE INDEX IF NOT EXISTS idx_report_schedule_type   ON report_schedules(report_type);
CREATE INDEX IF NOT EXISTS idx_report_schedule_status ON report_schedules(status);

-- ============================================================
-- Group 2: Activity/log tables (UUID refs, no FK constraints)
-- ============================================================

CREATE TABLE IF NOT EXISTS activity_logs (
    id             UUID         PRIMARY KEY,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    user_id        UUID,
    user_name      VARCHAR(255),
    user_email     VARCHAR(255),
    activity_type  VARCHAR(50),
    action         VARCHAR(50),
    status         VARCHAR(20),
    description    TEXT,
    old_values     TEXT,
    new_values     TEXT,
    entity_type    VARCHAR(100),
    entity_id      UUID,
    method_name    VARCHAR(200),
    class_name     VARCHAR(200),
    request_method VARCHAR(10),
    request_path   VARCHAR(500),
    ip_address     VARCHAR(50),
    user_agent     VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_activity_user    ON activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_type    ON activity_logs(activity_type);
CREATE INDEX IF NOT EXISTS idx_activity_created ON activity_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_activity_action  ON activity_logs(action);
CREATE INDEX IF NOT EXISTS idx_activity_status  ON activity_logs(status);
CREATE INDEX IF NOT EXISTS idx_activity_entity  ON activity_logs(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS security_activities (
    id             UUID        PRIMARY KEY,
    is_active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,
    user_id        UUID        NOT NULL,
    activity_type  VARCHAR(50) NOT NULL,
    description    TEXT,
    ip_address     VARCHAR(50),
    user_agent     VARCHAR(500),
    device_info    VARCHAR(255),
    location       VARCHAR(255),
    status         VARCHAR(20),
    failure_reason VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_security_user    ON security_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_security_type    ON security_activities(activity_type);
CREATE INDEX IF NOT EXISTS idx_security_created ON security_activities(created_at);

CREATE TABLE IF NOT EXISTS order_activity_logs (
    id            UUID        PRIMARY KEY,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    order_id      UUID        NOT NULL,
    user_id       UUID,
    activity_type VARCHAR(50) NOT NULL,
    description   TEXT,
    old_value     VARCHAR(255),
    new_value     VARCHAR(255),
    ip_address    VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_order_activity_log_order   ON order_activity_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_order_activity_log_user    ON order_activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_order_activity_log_created ON order_activity_logs(created_at);

CREATE TABLE IF NOT EXISTS payment_activity_logs (
    id             UUID          PRIMARY KEY,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    user_id        UUID,
    payment_id     UUID,
    activity_type  VARCHAR(50)   NOT NULL,
    description    TEXT,
    amount         DECIMAL(15,2),
    payment_method VARCHAR(50),
    status         VARCHAR(30),
    ip_address     VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_payment_activity_log_user    ON payment_activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_activity_log_payment ON payment_activity_logs(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_activity_log_created ON payment_activity_logs(created_at);

CREATE TABLE IF NOT EXISTS seller_promotion_activities (
    id             UUID          PRIMARY KEY,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    seller_id      UUID          NOT NULL,
    promotion_id   UUID,
    activity_type  VARCHAR(50)   NOT NULL,
    promotion_name VARCHAR(255),
    discount_value DECIMAL(10,2),
    description    TEXT,
    ip_address     VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_seller_promo_act_promo  ON seller_promotion_activities(promotion_id);
CREATE INDEX IF NOT EXISTS idx_seller_promo_act_seller ON seller_promotion_activities(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_promo_act_type   ON seller_promotion_activities(activity_type);

CREATE TABLE IF NOT EXISTS admin_promotion_activities (
    id             UUID          PRIMARY KEY,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    admin_id       UUID          NOT NULL,
    promotion_id   UUID,
    activity_type  VARCHAR(50)   NOT NULL,
    promotion_name VARCHAR(255),
    promo_code     VARCHAR(50),
    discount_value DECIMAL(10,2),
    description    TEXT,
    ip_address     VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_admin_promo_act_promo ON admin_promotion_activities(promotion_id);
CREATE INDEX IF NOT EXISTS idx_admin_promo_act_type  ON admin_promotion_activities(activity_type);

CREATE TABLE IF NOT EXISTS coupon_activities (
    id              UUID          PRIMARY KEY,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    user_id         UUID,
    coupon_id       UUID,
    seller_id       UUID,
    order_id        UUID,
    activity_type   VARCHAR(50)   NOT NULL,
    coupon_code     VARCHAR(50),
    discount_amount DECIMAL(10,2),
    order_amount    DECIMAL(10,2),
    description     TEXT,
    ip_address      VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_coupon_activity_user   ON coupon_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_coupon_activity_coupon ON coupon_activities(coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_activity_seller ON coupon_activities(seller_id);
CREATE INDEX IF NOT EXISTS idx_coupon_activity_type   ON coupon_activities(activity_type);

CREATE TABLE IF NOT EXISTS tag_activities (
    id            UUID        PRIMARY KEY,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    user_id       UUID,
    tag_id        UUID,
    product_id    UUID,
    activity_type VARCHAR(50) NOT NULL,
    tag_name      VARCHAR(100),
    description   TEXT,
    ip_address    VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_tag_activity_user ON tag_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_tag_activity_tag  ON tag_activities(tag_id);
CREATE INDEX IF NOT EXISTS idx_tag_activity_type ON tag_activities(activity_type);

CREATE TABLE IF NOT EXISTS flash_sale_activities (
    id               UUID        PRIMARY KEY,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,
    user_id          UUID,
    seller_id        UUID,
    flash_sale_id    UUID,
    application_id   UUID,
    activity_type    VARCHAR(50) NOT NULL,
    flash_sale_name  VARCHAR(200),
    description      TEXT,
    ip_address       VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_flash_sale_activity_user       ON flash_sale_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_flash_sale_activity_seller     ON flash_sale_activities(seller_id);
CREATE INDEX IF NOT EXISTS idx_flash_sale_activity_flash_sale ON flash_sale_activities(flash_sale_id);
CREATE INDEX IF NOT EXISTS idx_flash_sale_activity_type       ON flash_sale_activities(activity_type);

-- ============================================================
-- Group 3: UUID-ref tables (no FK constraints defined in entity)
-- ============================================================

CREATE TABLE IF NOT EXISTS password_history (
    id            UUID         PRIMARY KEY,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    user_id       UUID         NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    changed_at    TIMESTAMP    NOT NULL,
    ip_address    VARCHAR(50),
    is_current    BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_password_user ON password_history(user_id);

CREATE TABLE IF NOT EXISTS saved_payment_methods (
    id                          UUID         PRIMARY KEY,
    is_active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP    NOT NULL,
    updated_at                  TIMESTAMP    NOT NULL,
    user_id                     UUID         NOT NULL,
    method_type                 VARCHAR(30)  NOT NULL,
    provider                    VARCHAR(50),
    last_four                   VARCHAR(4),
    card_brand                  VARCHAR(20),
    expiry_month                INTEGER,
    expiry_year                 INTEGER,
    card_holder_name            VARCHAR(100),
    phone_number                VARCHAR(20),
    account_number              VARCHAR(20),
    bank_name                   VARCHAR(100),
    account_holder_name         VARCHAR(100),
    is_default                  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_verified                 BOOLEAN      NOT NULL DEFAULT FALSE,
    last_used_at                DATE,
    paystack_customer_id        VARCHAR(100),
    paystack_authorization_code VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_payment_method_user    ON saved_payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_method_default ON saved_payment_methods(is_default);

CREATE TABLE IF NOT EXISTS seller_coupons (
    id               UUID          PRIMARY KEY,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    seller_id        UUID          NOT NULL,
    code             VARCHAR(50)   NOT NULL,
    name             VARCHAR(200)  NOT NULL,
    description      TEXT,
    discount_type    VARCHAR(20)   NOT NULL,
    discount_value   DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    max_uses         INTEGER,
    usage_count      INTEGER       NOT NULL DEFAULT 0,
    max_uses_per_user INTEGER      DEFAULT 1,
    valid_from       TIMESTAMP     NOT NULL,
    valid_until      TIMESTAMP     NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
);
CREATE INDEX IF NOT EXISTS idx_seller_coupon_code   ON seller_coupons(code);
CREATE INDEX IF NOT EXISTS idx_seller_coupon_seller ON seller_coupons(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_coupon_status ON seller_coupons(status);

CREATE TABLE IF NOT EXISTS seller_promotions (
    id             UUID          PRIMARY KEY,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    seller_id      UUID          NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    promotion_type VARCHAR(30)   NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    discount_type  VARCHAR(20)   DEFAULT 'PERCENTAGE',
    min_purchase   DECIMAL(10,2),
    max_discount   DECIMAL(10,2),
    start_date     TIMESTAMP     NOT NULL,
    end_date       TIMESTAMP     NOT NULL,
    usage_limit    INTEGER,
    usage_count    INTEGER       NOT NULL DEFAULT 0,
    status         VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED'
);
CREATE INDEX IF NOT EXISTS idx_seller_promo_seller ON seller_promotions(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_promo_status ON seller_promotions(status);
CREATE INDEX IF NOT EXISTS idx_seller_promo_type   ON seller_promotions(promotion_type);

CREATE TABLE IF NOT EXISTS seller_flash_sale_applications (
    id                 UUID          PRIMARY KEY,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    flash_sale_id      UUID          NOT NULL,
    seller_id          UUID          NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reviewed_by        UUID,
    reviewed_at        TIMESTAMP,
    review_note        TEXT,
    orders_count       INTEGER       DEFAULT 0,
    revenue_generated  DECIMAL(15,2) DEFAULT 0,
    CONSTRAINT idx_seller_flash_sale_unique UNIQUE (flash_sale_id, seller_id)
);
CREATE INDEX IF NOT EXISTS idx_seller_flash_sale_app_seller     ON seller_flash_sale_applications(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_flash_sale_app_flash_sale ON seller_flash_sale_applications(flash_sale_id);

CREATE TABLE IF NOT EXISTS seller_flash_sale_products (
    id               UUID          PRIMARY KEY,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    application_id   UUID          NOT NULL,
    product_id       UUID          NOT NULL,
    original_price   DECIMAL(10,2) NOT NULL,
    discounted_price DECIMAL(10,2),
    quantity         INTEGER       DEFAULT 0,
    sold_count       INTEGER       DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_flash_sale_product_app     ON seller_flash_sale_products(application_id);
CREATE INDEX IF NOT EXISTS idx_flash_sale_product_product ON seller_flash_sale_products(product_id);

-- ============================================================
-- Group 4: FK to users
-- ============================================================

CREATE TABLE IF NOT EXISTS seller_profiles (
    id                    UUID          PRIMARY KEY,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    user_id               UUID          NOT NULL UNIQUE REFERENCES users(id),
    store_name            VARCHAR(255)  NOT NULL,
    store_description     TEXT,
    store_website         VARCHAR(500),
    store_logo            VARCHAR(500),
    store_banner          VARCHAR(500),
    email                 VARCHAR(255),
    phone                 VARCHAR(50),
    region                VARCHAR(50),
    city                  VARCHAR(100),
    business_address      VARCHAR(500),
    working_hours         VARCHAR(255),
    facebook_url          VARCHAR(500),
    instagram_url         VARCHAR(500),
    twitter_url           VARCHAR(500),
    rating                DECIMAL(3,2)  DEFAULT 0,
    total_reviews         INTEGER       DEFAULT 0,
    total_products        INTEGER       DEFAULT 0,
    total_sales           INTEGER       DEFAULT 0,
    total_revenue         DECIMAL(12,2) DEFAULT 0,
    verification_status   VARCHAR(50)   DEFAULT 'PENDING',
    seller_status         VARCHAR(50)   DEFAULT 'PENDING',
    business_registration VARCHAR(500),
    tax_id                VARCHAR(100),
    bank_name             VARCHAR(255),
    account_holder_name   VARCHAR(255),
    account_number        VARCHAR(100),
    branch                VARCHAR(255),
    payout_schedule       VARCHAR(50)   DEFAULT 'MONTHLY',
    return_policy         TEXT
);
CREATE INDEX IF NOT EXISTS idx_seller_user_id     ON seller_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_seller_verification ON seller_profiles(verification_status);
CREATE INDEX IF NOT EXISTS idx_seller_status      ON seller_profiles(seller_status);

CREATE TABLE IF NOT EXISTS customer_profiles (
    id                UUID          PRIMARY KEY,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    user_id           UUID          NOT NULL UNIQUE REFERENCES users(id),
    loyalty_points    INTEGER       DEFAULT 0,
    membership_status VARCHAR(50)   DEFAULT 'BRONZE',
    total_orders      INTEGER       DEFAULT 0,
    total_spent       DECIMAL(12,2) DEFAULT 0,
    preferred_currency VARCHAR(3)   DEFAULT 'USD',
    newsletter        BOOLEAN       DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS two_factor_auth (
    id                UUID         PRIMARY KEY,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    user_id           UUID         NOT NULL UNIQUE,
    secret_key        VARCHAR(255) NOT NULL,
    is_enabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled_at        TIMESTAMP,
    authenticator_type VARCHAR(20) DEFAULT 'TOTP',
    recovery_codes    TEXT,
    backup_codes_used INTEGER      NOT NULL DEFAULT 0,
    last_verified_at  TIMESTAMP,
    failed_attempts   INTEGER      NOT NULL DEFAULT 0,
    locked_until      TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_2fa_user ON two_factor_auth(user_id);

CREATE TABLE IF NOT EXISTS notification_settings (
    id                   UUID    PRIMARY KEY,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    user_id              UUID    NOT NULL UNIQUE,
    order_updates        BOOLEAN NOT NULL DEFAULT TRUE,
    payment_confirmation BOOLEAN NOT NULL DEFAULT TRUE,
    shipping_updates     BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_email    BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_sms      BOOLEAN NOT NULL DEFAULT FALSE,
    new_product_alerts   BOOLEAN NOT NULL DEFAULT TRUE,
    price_drop_alerts    BOOLEAN NOT NULL DEFAULT TRUE,
    wishlist_updates     BOOLEAN NOT NULL DEFAULT TRUE,
    review_requests      BOOLEAN NOT NULL DEFAULT TRUE,
    newsletter           BOOLEAN NOT NULL DEFAULT FALSE,
    browser_push         BOOLEAN NOT NULL DEFAULT TRUE,
    app_push             BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_notif_settings_user ON notification_settings(user_id);

CREATE TABLE IF NOT EXISTS wishlists (
    id         UUID      PRIMARY KEY,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id    UUID      NOT NULL UNIQUE REFERENCES users(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_wishlist_user ON wishlists(user_id);

CREATE TABLE IF NOT EXISTS auth_sessions (
    id               UUID         PRIMARY KEY,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    user_id          UUID         NOT NULL REFERENCES users(id),
    refresh_token    VARCHAR(500) NOT NULL UNIQUE,
    access_token     VARCHAR(500),
    token_type       VARCHAR(20)  DEFAULT 'Bearer',
    device_name      VARCHAR(100),
    ip_address       VARCHAR(50),
    user_agent       VARCHAR(500),
    expires_at       TIMESTAMP    NOT NULL,
    logged_out_at    TIMESTAMP,
    last_activity_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS        idx_auth_user    ON auth_sessions(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_token   ON auth_sessions(refresh_token);
CREATE INDEX IF NOT EXISTS        idx_auth_active  ON auth_sessions(is_active);
CREATE INDEX IF NOT EXISTS        idx_auth_expires ON auth_sessions(expires_at);

CREATE TABLE IF NOT EXISTS addresses (
    id             UUID         PRIMARY KEY,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    user_id        UUID         NOT NULL REFERENCES users(id),
    label          VARCHAR(100),
    address_type   VARCHAR(30)  DEFAULT 'SHIPPING',
    street_address VARCHAR(255) NOT NULL,
    city           VARCHAR(100) NOT NULL,
    state          VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    country        VARCHAR(100) NOT NULL,
    is_default     BOOLEAN      DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_addresses_user_id    ON addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_addresses_is_default ON addresses(is_default);

CREATE TABLE IF NOT EXISTS notifications (
    id                UUID         PRIMARY KEY,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    user_id           UUID         NOT NULL REFERENCES users(id),
    type              VARCHAR(50)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    message           TEXT,
    related_entity_id UUID,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notifications(is_read);

-- ============================================================
-- Group 5: FK to seller_profiles
-- ============================================================

CREATE TABLE IF NOT EXISTS seller_notification_settings (
    id                 UUID    PRIMARY KEY,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    seller_id          UUID    NOT NULL UNIQUE REFERENCES seller_profiles(id),
    new_orders         BOOLEAN NOT NULL DEFAULT TRUE,
    order_updates      BOOLEAN NOT NULL DEFAULT TRUE,
    customer_messages  BOOLEAN NOT NULL DEFAULT TRUE,
    stock_alerts       BOOLEAN NOT NULL DEFAULT TRUE,
    payment_updates    BOOLEAN NOT NULL DEFAULT TRUE,
    refund_requests    BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_emails BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_seller_notif_seller ON seller_notification_settings(seller_id);

CREATE TABLE IF NOT EXISTS shipping_zones (
    id                UUID          PRIMARY KEY,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    seller_id         UUID          NOT NULL REFERENCES seller_profiles(id),
    zone_name         VARCHAR(100)  NOT NULL,
    zone_description  VARCHAR(255),
    region            VARCHAR(50),
    delivery_method   VARCHAR(30)   DEFAULT 'DIRECT_ADDRESS',
    shipping_cost     DECIMAL(10,2) NOT NULL DEFAULT 0,
    free_shipping_min DECIMAL(10,2),
    estimated_days    VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_shipping_zone_seller ON shipping_zones(seller_id);
CREATE INDEX IF NOT EXISTS idx_shipping_zone_region ON shipping_zones(region);

-- ============================================================
-- Group 6: FK to categories + users (products)
-- ============================================================

CREATE TABLE IF NOT EXISTS products (
    id                 UUID          PRIMARY KEY,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    name               VARCHAR(255)  NOT NULL,
    slug               VARCHAR(255)  NOT NULL UNIQUE,
    brand              VARCHAR(255),
    sku                VARCHAR(255),
    description        TEXT,
    price              DECIMAL(10,2) NOT NULL,
    original_price     DECIMAL(10,2),
    discount           DECIMAL(10,2),
    stock              INTEGER,
    available_quantity INTEGER       NOT NULL DEFAULT 0,
    sold_quantity      INTEGER       NOT NULL DEFAULT 0,
    reserved_quantity  INTEGER       NOT NULL DEFAULT 0,
    rating             DECIMAL(3,2),
    review_count       INTEGER,
    view_count         INTEGER       DEFAULT 0,
    category_id        UUID          REFERENCES categories(id),
    seller_id          UUID          REFERENCES users(id),
    status             VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    is_approved        BOOLEAN       NOT NULL DEFAULT FALSE,
    inventory_status   VARCHAR(50)   DEFAULT 'IN_STOCK',
    featured           BOOLEAN       NOT NULL DEFAULT FALSE,
    is_new             BOOLEAN       NOT NULL DEFAULT FALSE,
    is_bestseller      BOOLEAN       NOT NULL DEFAULT FALSE,
    main_image_url     VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_product_seller_id   ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_product_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_product_status      ON products(status);
CREATE INDEX IF NOT EXISTS idx_product_price       ON products(price);
CREATE INDEX IF NOT EXISTS idx_product_rating      ON products(rating);
CREATE INDEX IF NOT EXISTS idx_product_brand       ON products(brand);

-- ============================================================
-- Group 7: FK to products
-- ============================================================

CREATE TABLE IF NOT EXISTS product_images (
    id         UUID         PRIMARY KEY,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    product_id UUID         NOT NULL REFERENCES products(id),
    image_url  VARCHAR(500) NOT NULL,
    alt        VARCHAR(255),
    ordering   INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_variants (
    id             UUID          PRIMARY KEY,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    product_id     UUID          NOT NULL REFERENCES products(id),
    sku            VARCHAR(255)  NOT NULL,
    size           VARCHAR(50),
    color          VARCHAR(50),
    price_override DECIMAL(10,2),
    stock          INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_variant_sku UNIQUE (sku)
);

-- ============================================================
-- Group 8: carts (user_id UUID stored, OneToOne relation)
-- ============================================================

CREATE TABLE IF NOT EXISTS carts (
    id               UUID        PRIMARY KEY,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,
    user_id          UUID        NOT NULL UNIQUE,
    coupon_code      VARCHAR(50),
    is_checked_out   BOOLEAN     DEFAULT FALSE,
    checked_out_at   TIMESTAMP,
    is_abandoned     BOOLEAN     DEFAULT FALSE,
    abandoned_at     TIMESTAMP,
    last_activity_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cart_user ON carts(user_id);

-- ============================================================
-- Group 9: wishlist_items
-- ============================================================

CREATE TABLE IF NOT EXISTS wishlist_items (
    id                   UUID          PRIMARY KEY,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL,
    wishlist_id          UUID          NOT NULL REFERENCES wishlists(id),
    user_id              UUID          NOT NULL REFERENCES users(id),
    product_id           UUID          NOT NULL REFERENCES products(id),
    added_at             TIMESTAMP     NOT NULL,
    priority             VARCHAR(20)   DEFAULT 'MEDIUM',
    notes                VARCHAR(1000),
    desired_quantity     INTEGER       DEFAULT 1,
    target_price         DECIMAL(10,2),
    notify_on_price_drop BOOLEAN       DEFAULT TRUE,
    notify_on_stock      BOOLEAN       DEFAULT TRUE,
    is_public            BOOLEAN       DEFAULT FALSE,
    collection_name      VARCHAR(255),
    purchased            BOOLEAN       DEFAULT FALSE,
    price_dropped        BOOLEAN       DEFAULT FALSE,
    purchased_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wishlist_item_user     ON wishlist_items(user_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_item_product  ON wishlist_items(product_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_item_wishlist ON wishlist_items(wishlist_id);

-- ============================================================
-- Group 10: reviews
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id                UUID        PRIMARY KEY,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL,
    product_id        UUID        NOT NULL REFERENCES products(id),
    customer_id       UUID        NOT NULL REFERENCES users(id),
    rating            INTEGER     NOT NULL,
    title             VARCHAR(200),
    comment           TEXT,
    has_images        BOOLEAN     DEFAULT FALSE,
    pros              VARCHAR(255),
    cons              VARCHAR(255),
    helpful           INTEGER     DEFAULT 0,
    unhelpful         INTEGER     DEFAULT 0,
    verified_purchase BOOLEAN     NOT NULL DEFAULT FALSE,
    approved          BOOLEAN     DEFAULT FALSE,
    admin_response    TEXT,
    admin_response_at TIMESTAMP,
    admin_response_by UUID,
    seller_reply      TEXT,
    seller_replied_at TIMESTAMP,
    rejection_reason  VARCHAR(255),
    deleted           BOOLEAN     DEFAULT FALSE,
    deleted_at        TIMESTAMP,
    CONSTRAINT uk_product_customer UNIQUE (product_id, customer_id)
);
CREATE INDEX IF NOT EXISTS idx_review_product  ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_review_customer ON reviews(customer_id);
CREATE INDEX IF NOT EXISTS idx_review_rating   ON reviews(rating);

-- ============================================================
-- Group 11: cart_items
-- ============================================================

CREATE TABLE IF NOT EXISTS cart_items (
    id         UUID          PRIMARY KEY,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL,
    cart_id    UUID          NOT NULL REFERENCES carts(id),
    product_id UUID          NOT NULL REFERENCES products(id),
    variant_id UUID          REFERENCES product_variants(id),
    quantity   INTEGER       NOT NULL DEFAULT 1,
    price      DECIMAL(10,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_cart_item_cart    ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_product ON cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_variant ON cart_items(variant_id);

-- ============================================================
-- Group 12: orders
-- ============================================================

CREATE TABLE IF NOT EXISTS orders (
    id                  UUID          PRIMARY KEY,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    order_number        VARCHAR(50)   NOT NULL UNIQUE,
    customer_id         UUID          NOT NULL REFERENCES users(id),
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax                 DECIMAL(10,2) DEFAULT 0,
    shipping_cost       DECIMAL(10,2) DEFAULT 0,
    discount            DECIMAL(10,2) DEFAULT 0,
    total_amount        DECIMAL(10,2) NOT NULL DEFAULT 0,
    payment_method      VARCHAR(30),
    payment_status      VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    shipping_address_id UUID          REFERENCES addresses(id),
    billing_address_id  UUID          REFERENCES addresses(id),
    tracking_number     VARCHAR(100),
    estimated_delivery  TIMESTAMP,
    notes               TEXT,
    coupon_code         VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS        idx_order_customer_id   ON orders(customer_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_order_order_number  ON orders(order_number);
CREATE INDEX IF NOT EXISTS        idx_order_status        ON orders(status);
CREATE INDEX IF NOT EXISTS        idx_order_payment_status ON orders(payment_status);

-- ============================================================
-- Group 13: order_items
-- ============================================================

CREATE TABLE IF NOT EXISTS order_items (
    id         UUID          PRIMARY KEY,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL,
    order_id   UUID          NOT NULL REFERENCES orders(id),
    product_id UUID          NOT NULL REFERENCES products(id),
    variant_id UUID          REFERENCES product_variants(id),
    seller_id  UUID          REFERENCES users(id),
    quantity   INTEGER       NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    subtotal   DECIMAL(10,2) NOT NULL,
    size       VARCHAR(50),
    color      VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_id ON order_items(product_id);

-- ============================================================
-- Group 14: order_timeline + refunds
-- ============================================================

CREATE TABLE IF NOT EXISTS order_timeline (
    id         UUID        PRIMARY KEY,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    order_id   UUID        NOT NULL REFERENCES orders(id),
    status     VARCHAR(30) NOT NULL,
    message    TEXT,
    timestamp  TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_timeline_order_id ON order_timeline(order_id);

CREATE TABLE IF NOT EXISTS refunds (
    id               UUID          PRIMARY KEY,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    refund_number    VARCHAR(50)   NOT NULL UNIQUE,
    order_id         UUID          NOT NULL REFERENCES orders(id),
    customer_id      UUID          NOT NULL,
    seller_id        UUID,
    amount           DECIMAL(10,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reason           VARCHAR(50)   NOT NULL,
    customer_note    TEXT,
    admin_note       TEXT,
    rejection_reason VARCHAR(255),
    reviewed_at      TIMESTAMP,
    reviewed_by      UUID,
    completed_at     TIMESTAMP,
    transaction_id   VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_refund_order_id    ON refunds(order_id);
CREATE INDEX IF NOT EXISTS idx_refund_status      ON refunds(status);
CREATE INDEX IF NOT EXISTS idx_refund_customer_id ON refunds(customer_id);
CREATE INDEX IF NOT EXISTS idx_refund_seller_id   ON refunds(seller_id);
CREATE INDEX IF NOT EXISTS idx_refund_created_at  ON refunds(created_at);

-- ============================================================
-- Group 15: payment_transactions + coupon_usages
-- ============================================================

CREATE TABLE IF NOT EXISTS payment_transactions (
    id                  UUID          PRIMARY KEY,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    order_id            UUID          NOT NULL REFERENCES orders(id),
    customer_id         UUID          NOT NULL REFERENCES users(id),
    amount              DECIMAL(10,2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
    payment_method      VARCHAR(30)   NOT NULL,
    transaction_id      VARCHAR(255),
    paystack_reference  VARCHAR(100),
    gateway_response    VARCHAR(1000),
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(500),
    idempotency_key     VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_payment_order          ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_customer       ON payment_transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_status         ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_id ON payment_transactions(transaction_id);

CREATE TABLE IF NOT EXISTS coupon_usages (
    id              UUID          PRIMARY KEY,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    coupon_id       UUID          NOT NULL REFERENCES coupons(id),
    user_id         UUID          NOT NULL REFERENCES users(id),
    order_id        UUID          NOT NULL REFERENCES orders(id),
    discount_amount DECIMAL(10,2) NOT NULL,
    used_at         TIMESTAMP     NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_coupon_id ON coupon_usages(coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_user_id   ON coupon_usages(user_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_order_id  ON coupon_usages(order_id);

-- ============================================================
-- Group 16: stock_reservations
-- ============================================================

CREATE TABLE IF NOT EXISTS stock_reservations (
    id            UUID        PRIMARY KEY,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    cart_item_id  UUID        NOT NULL UNIQUE REFERENCES cart_items(id),
    product_id    UUID        NOT NULL REFERENCES products(id),
    variant_id    UUID        REFERENCES product_variants(id),
    quantity      INTEGER     NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count   INTEGER     DEFAULT 0,
    error_message VARCHAR(500),
    expires_at    TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS        idx_stock_reservation_expires   ON stock_reservations(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_stock_reservation_cart_item ON stock_reservations(cart_item_id);

-- ============================================================
-- Group 17: promotion_participating_sellers
-- ============================================================

CREATE TABLE IF NOT EXISTS promotion_participating_sellers (
    id           UUID      PRIMARY KEY,
    is_active    BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    promotion_id UUID      NOT NULL REFERENCES promotions(id),
    seller_id    UUID      NOT NULL,
    joined_at    TIMESTAMP,
    status       VARCHAR(20) DEFAULT 'ACTIVE'
);
CREATE INDEX IF NOT EXISTS idx_participating_seller    ON promotion_participating_sellers(seller_id);
CREATE INDEX IF NOT EXISTS idx_participating_promotion ON promotion_participating_sellers(promotion_id);

-- ============================================================
-- Group 18: store_follows
-- ============================================================

CREATE TABLE IF NOT EXISTS store_follows (
    id          UUID      PRIMARY KEY,
    is_active   BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    customer_id UUID      NOT NULL REFERENCES users(id),
    seller_id   UUID      NOT NULL REFERENCES seller_profiles(id),
    followed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_store_follow_customer_seller UNIQUE (customer_id, seller_id)
);
CREATE INDEX IF NOT EXISTS idx_store_follow_customer ON store_follows(customer_id);
CREATE INDEX IF NOT EXISTS idx_store_follow_seller   ON store_follows(seller_id);

-- ============================================================
-- Group 19: Notification activity tables (UUID refs, no FK)
-- ============================================================

CREATE TABLE IF NOT EXISTS notification_preference_activities (
    id           UUID        PRIMARY KEY,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    user_id      UUID        NOT NULL,
    setting_name VARCHAR(50) NOT NULL,
    old_value    VARCHAR(50),
    new_value    VARCHAR(50),
    channel      VARCHAR(20),
    ip_address   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_pref_user    ON notification_preference_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_pref_setting ON notification_preference_activities(setting_name);
CREATE INDEX IF NOT EXISTS idx_pref_created ON notification_preference_activities(created_at);

CREATE TABLE IF NOT EXISTS unified_notification_activities (
    id                UUID         PRIMARY KEY,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    user_id           UUID         NOT NULL,
    notification_id   UUID,
    activity_type     VARCHAR(50)  NOT NULL,
    category          VARCHAR(20)  NOT NULL,
    notification_type VARCHAR(30),
    title             VARCHAR(255),
    message           TEXT,
    related_id        UUID,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at           TIMESTAMP,
    is_pinned         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_archived       BOOLEAN      NOT NULL DEFAULT FALSE,
    sender_type       VARCHAR(20),
    sender_id         UUID
);
CREATE INDEX IF NOT EXISTS idx_unified_user     ON unified_notification_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_unified_type     ON unified_notification_activities(activity_type);
CREATE INDEX IF NOT EXISTS idx_unified_category ON unified_notification_activities(category);
CREATE INDEX IF NOT EXISTS idx_unified_created  ON unified_notification_activities(created_at);

-- ============================================================
-- Group 20: Message tables
-- ============================================================

CREATE TABLE IF NOT EXISTS conversations (
    id                   UUID         PRIMARY KEY,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    participant_id       UUID,
    participant_type     VARCHAR(20),
    subject              VARCHAR(255),
    category             VARCHAR(30),
    priority             VARCHAR(20)  DEFAULT 'MEDIUM',
    status               VARCHAR(20)  DEFAULT 'OPEN',
    type                 VARCHAR(20)  DEFAULT 'CUSTOMER',
    is_starred           BOOLEAN      DEFAULT FALSE,
    is_pinned            BOOLEAN      DEFAULT FALSE,
    last_message_preview VARCHAR(255),
    unread_count         INTEGER      DEFAULT 0,
    order_id             UUID,
    product_id           UUID
);
CREATE INDEX IF NOT EXISTS idx_conversation_status      ON conversations(status);
CREATE INDEX IF NOT EXISTS idx_conversation_priority    ON conversations(priority);
CREATE INDEX IF NOT EXISTS idx_conversation_type        ON conversations(type);
CREATE INDEX IF NOT EXISTS idx_conversation_participant ON conversations(participant_id);

CREATE TABLE IF NOT EXISTS messages (
    id                UUID         PRIMARY KEY,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    conversation_id   UUID         NOT NULL REFERENCES conversations(id),
    sender_id         UUID,
    sender_type       VARCHAR(20),
    sender_name       VARCHAR(100),
    content           TEXT         NOT NULL,
    is_read           BOOLEAN      DEFAULT FALSE,
    read_at           TIMESTAMP,
    is_system_message BOOLEAN      DEFAULT FALSE,
    attachment_url    VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_message_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_message_sender       ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_message_read         ON messages(is_read);

-- ============================================================
-- Group 21: delivery_fees (FK to delivery_regions)
-- ============================================================

CREATE TABLE IF NOT EXISTS delivery_fees (
    id              UUID          PRIMARY KEY,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    town_name       VARCHAR(100)  NOT NULL,
    delivery_method VARCHAR(30)   NOT NULL DEFAULT 'DIRECT_ADDRESS',
    base_fee        DECIMAL(10,2) NOT NULL DEFAULT 0,
    per_km_fee      DECIMAL(10,2) NOT NULL DEFAULT 0,
    estimated_days  INTEGER       NOT NULL DEFAULT 1,
    region_id       UUID          REFERENCES delivery_regions(id)
);
CREATE INDEX IF NOT EXISTS idx_delivery_town   ON delivery_fees(town_name);
CREATE INDEX IF NOT EXISTS idx_delivery_region ON delivery_fees(region_id);

-- ============================================================
-- Group 22: seller_product_tags (UUID refs, no FK constraints)
-- ============================================================

CREATE TABLE IF NOT EXISTS seller_product_tags (
    id         UUID      PRIMARY KEY,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    seller_id  UUID      NOT NULL,
    product_id UUID      NOT NULL,
    tag_id     UUID      NOT NULL,
    CONSTRAINT uk_seller_product_tag UNIQUE (product_id, tag_id)
);
CREATE INDEX IF NOT EXISTS idx_seller_product_tag_seller  ON seller_product_tags(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_product_tag_product ON seller_product_tags(product_id);
CREATE INDEX IF NOT EXISTS idx_seller_product_tag_tag     ON seller_product_tags(tag_id);

--rollback DROP TABLE IF EXISTS seller_product_tags CASCADE;
--rollback DROP TABLE IF EXISTS delivery_fees CASCADE;
--rollback DROP TABLE IF EXISTS messages CASCADE;
--rollback DROP TABLE IF EXISTS conversations CASCADE;
--rollback DROP TABLE IF EXISTS unified_notification_activities CASCADE;
--rollback DROP TABLE IF EXISTS notification_preference_activities CASCADE;
--rollback DROP TABLE IF EXISTS store_follows CASCADE;
--rollback DROP TABLE IF EXISTS promotion_participating_sellers CASCADE;
--rollback DROP TABLE IF EXISTS stock_reservations CASCADE;
--rollback DROP TABLE IF EXISTS coupon_usages CASCADE;
--rollback DROP TABLE IF EXISTS payment_transactions CASCADE;
--rollback DROP TABLE IF EXISTS refunds CASCADE;
--rollback DROP TABLE IF EXISTS order_timeline CASCADE;
--rollback DROP TABLE IF EXISTS order_items CASCADE;
--rollback DROP TABLE IF EXISTS orders CASCADE;
--rollback DROP TABLE IF EXISTS cart_items CASCADE;
--rollback DROP TABLE IF EXISTS reviews CASCADE;
--rollback DROP TABLE IF EXISTS wishlist_items CASCADE;
--rollback DROP TABLE IF EXISTS carts CASCADE;
--rollback DROP TABLE IF EXISTS product_variants CASCADE;
--rollback DROP TABLE IF EXISTS product_images CASCADE;
--rollback DROP TABLE IF EXISTS products CASCADE;
--rollback DROP TABLE IF EXISTS shipping_zones CASCADE;
--rollback DROP TABLE IF EXISTS seller_notification_settings CASCADE;
--rollback DROP TABLE IF EXISTS notifications CASCADE;
--rollback DROP TABLE IF EXISTS addresses CASCADE;
--rollback DROP TABLE IF EXISTS auth_sessions CASCADE;
--rollback DROP TABLE IF EXISTS wishlists CASCADE;
--rollback DROP TABLE IF EXISTS notification_settings CASCADE;
--rollback DROP TABLE IF EXISTS two_factor_auth CASCADE;
--rollback DROP TABLE IF EXISTS customer_profiles CASCADE;
--rollback DROP TABLE IF EXISTS seller_profiles CASCADE;
--rollback DROP TABLE IF EXISTS seller_flash_sale_products CASCADE;
--rollback DROP TABLE IF EXISTS seller_flash_sale_applications CASCADE;
--rollback DROP TABLE IF EXISTS seller_promotions CASCADE;
--rollback DROP TABLE IF EXISTS seller_coupons CASCADE;
--rollback DROP TABLE IF EXISTS saved_payment_methods CASCADE;
--rollback DROP TABLE IF EXISTS password_history CASCADE;
--rollback DROP TABLE IF EXISTS flash_sale_activities CASCADE;
--rollback DROP TABLE IF EXISTS tag_activities CASCADE;
--rollback DROP TABLE IF EXISTS coupon_activities CASCADE;
--rollback DROP TABLE IF EXISTS admin_promotion_activities CASCADE;
--rollback DROP TABLE IF EXISTS seller_promotion_activities CASCADE;
--rollback DROP TABLE IF EXISTS payment_activity_logs CASCADE;
--rollback DROP TABLE IF EXISTS order_activity_logs CASCADE;
--rollback DROP TABLE IF EXISTS security_activities CASCADE;
--rollback DROP TABLE IF EXISTS activity_logs CASCADE;
--rollback DROP TABLE IF EXISTS report_schedules CASCADE;
--rollback DROP TABLE IF EXISTS reports CASCADE;
--rollback DROP TABLE IF EXISTS promotions CASCADE;
--rollback DROP TABLE IF EXISTS admin_promotions CASCADE;
--rollback DROP TABLE IF EXISTS admin_flash_sales CASCADE;
--rollback DROP TABLE IF EXISTS search_analytics CASCADE;
--rollback DROP TABLE IF EXISTS contact_messages CASCADE;
--rollback DROP TABLE IF EXISTS faqs CASCADE;
--rollback DROP TABLE IF EXISTS subscribers CASCADE;
--rollback DROP TABLE IF EXISTS coupons CASCADE;
--rollback DROP TABLE IF EXISTS social_links CASCADE;
--rollback DROP TABLE IF EXISTS site_settings CASCADE;
--rollback DROP TABLE IF EXISTS tags CASCADE;
--rollback DROP TABLE IF EXISTS delivery_regions CASCADE;
--rollback DROP TABLE IF EXISTS categories CASCADE;
--rollback DROP TABLE IF EXISTS users CASCADE;
