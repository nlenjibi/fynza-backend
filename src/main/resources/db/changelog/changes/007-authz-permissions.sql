--liquibase formatted sql
--changeset fynza:007-authz-permissions dbms:postgresql
CREATE TABLE IF NOT EXISTS permissions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    resource        VARCHAR(50)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    description     TEXT,
    system_defined  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_permissions_code     ON permissions (code);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions (resource);
INSERT INTO permissions (code, resource, action, description) VALUES
    ('product.create','product','create','Create a new product'),
    ('product.read','product','read','Read product details'),
    ('product.update','product','update','Update an existing product'),
    ('product.delete','product','delete','Delete a product'),
    ('product.approve','product','approve','Approve a product for listing'),
    ('user.view','user','view','View user profile'),
    ('user.suspend','user','suspend','Suspend a user account'),
    ('user.manage','user','manage','Full user management access'),
    ('store.view','store','view','View store details'),
    ('store.manage','store','manage','Manage store settings'),
    ('order.view','order','view','View orders'),
    ('order.manage','order','manage','Manage order lifecycle'),
    ('order.refund','order','refund','Initiate order refunds'),
    ('payment.view','payment','view','View payment records'),
    ('payment.refund','payment','refund','Process payment refunds'),
    ('role.assign','role','assign','Assign roles to users'),
    ('role.revoke','role','revoke','Revoke roles from users'),
    ('role.manage','role','manage','Full role management access'),
    ('permission.grant','permission','grant','Grant permissions to roles'),
    ('permission.revoke','permission','revoke','Revoke permissions from roles'),
    ('report.view','report','view','View reports'),
    ('report.export','report','export','Export reports'),
    ('category.manage','category','manage','Manage product categories'),
    ('inventory.view','inventory','view','View inventory'),
    ('inventory.manage','inventory','manage','Manage inventory levels');
--rollback DROP TABLE IF EXISTS permissions;
