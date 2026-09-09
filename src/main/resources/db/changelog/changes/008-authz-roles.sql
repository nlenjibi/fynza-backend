--liquibase formatted sql
--changeset fynza:008-authz-roles dbms:postgresql
CREATE TABLE IF NOT EXISTS roles (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(200),
    description     TEXT,
    system_defined  BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_roles_code   ON roles (code);
CREATE INDEX IF NOT EXISTS idx_roles_active ON roles (active);
INSERT INTO roles (code, display_name, description, system_defined) VALUES
    ('ADMIN','Administrator','Platform administrator with broad access',TRUE),
    ('SELLER','Seller','Marketplace seller with store management permissions',TRUE),
    ('CUSTOMER','Customer','Regular buyer with self-service permissions',TRUE),
    ('SELLER_MANAGER','Seller Manager','Store manager appointed by a seller owner',TRUE),
    ('SUPPORT_AGENT','Support Agent','Customer support staff',TRUE),
    ('FINANCE_STAFF','Finance Staff','Finance and payment operations staff',TRUE);
CREATE TABLE IF NOT EXISTS role_permissions (
    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id         UUID      NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID      NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_by      UUID,
    granted_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions (role_id);
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('product.create','product.read','product.update','product.delete',
                  'store.view','store.manage','order.view','order.manage',
                  'inventory.view','inventory.manage')
WHERE r.code = 'SELLER';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('product.read','order.view')
WHERE r.code = 'CUSTOMER';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('product.create','product.read','product.update','store.view',
                  'order.view','order.manage','inventory.view','inventory.manage')
WHERE r.code = 'SELLER_MANAGER';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('user.view','order.view')
WHERE r.code = 'SUPPORT_AGENT';
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('payment.view','payment.refund','order.refund','report.view','report.export')
WHERE r.code = 'FINANCE_STAFF';
--rollback DROP TABLE IF EXISTS role_permissions; DROP TABLE IF EXISTS roles;
