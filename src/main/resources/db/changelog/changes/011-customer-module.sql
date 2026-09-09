--liquibase formatted sql
--changeset fynza:011-customer-module dbms:postgresql

CREATE SEQUENCE IF NOT EXISTS customer_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS customers (
    id               BIGSERIAL    PRIMARY KEY,
    public_id        UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL UNIQUE REFERENCES users(id),
    customer_number  VARCHAR(20)  NOT NULL UNIQUE,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PROSPECT',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_customers_user_id   ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_status    ON customers(status);
CREATE INDEX IF NOT EXISTS idx_customers_number    ON customers(customer_number);
CREATE INDEX IF NOT EXISTS idx_customers_is_active ON customers(is_active);

CREATE TABLE IF NOT EXISTS customer_preferences (
    id                   BIGSERIAL   PRIMARY KEY,
    public_id            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    customer_id          BIGINT      NOT NULL UNIQUE REFERENCES customers(id),
    language             VARCHAR(10) NOT NULL DEFAULT 'en',
    currency             VARCHAR(3)  NOT NULL DEFAULT 'GHS',
    marketing_opt_in     BOOLEAN     NOT NULL DEFAULT FALSE,
    email_notifications  BOOLEAN     NOT NULL DEFAULT TRUE,
    sms_notifications    BOOLEAN     NOT NULL DEFAULT FALSE,
    push_notifications   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_customer_pref_customer_id ON customer_preferences(customer_id);

CREATE TABLE IF NOT EXISTS customer_addresses (
    id              BIGSERIAL      PRIMARY KEY,
    public_id       UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    customer_id     BIGINT         NOT NULL REFERENCES customers(id),
    recipient_name  VARCHAR(150)   NOT NULL,
    phone_number    VARCHAR(30),
    address_line1   VARCHAR(255)   NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100)   NOT NULL,
    region          VARCHAR(100),
    country         VARCHAR(100)   NOT NULL,
    postal_code     VARCHAR(20),
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    address_type    VARCHAR(20)    NOT NULL DEFAULT 'HOME',
    is_default      BOOLEAN        NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_cust_addr_customer_id ON customer_addresses(customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_addr_is_default  ON customer_addresses(customer_id, is_default) WHERE is_default = TRUE;
CREATE INDEX IF NOT EXISTS idx_cust_addr_is_active   ON customer_addresses(customer_id, is_active) WHERE is_active = TRUE;

CREATE TABLE IF NOT EXISTS customer_status_history (
    id               BIGSERIAL   PRIMARY KEY,
    public_id        UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    customer_id      BIGINT      NOT NULL REFERENCES customers(id),
    previous_status  VARCHAR(30),
    new_status       VARCHAR(30) NOT NULL,
    reason           TEXT,
    changed_by       UUID,
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),
    expires_at       TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cust_status_hist_customer ON customer_status_history(customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_status_hist_created  ON customer_status_history(created_at);

--rollback DROP TABLE IF EXISTS customer_status_history;
--rollback DROP TABLE IF EXISTS customer_addresses;
--rollback DROP TABLE IF EXISTS customer_preferences;
--rollback DROP TABLE IF EXISTS customers;
--rollback DROP SEQUENCE IF EXISTS customer_number_seq;
