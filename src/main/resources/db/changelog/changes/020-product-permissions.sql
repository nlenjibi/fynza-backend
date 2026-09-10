--liquibase formatted sql
--changeset fynza:020-product-permissions dbms:postgresql

INSERT INTO permissions (code, resource, action, description) VALUES
    ('product.read.own',    'product', 'read.own',    'View own products'),
    ('product.create.own',  'product', 'create.own',  'Create a product in own store'),
    ('product.update.own',  'product', 'update.own',  'Update own product'),
    ('product.delete.own',  'product', 'delete.own',  'Soft-delete (archive) own product'),
    ('product.publish.own', 'product', 'publish.own', 'Publish own product'),
    ('product.archive.own', 'product', 'archive.own', 'Archive own product'),
    ('product.manage.own',  'product', 'manage.own',  'Full self-service product management'),
    ('product.read',        'product', 'read',        'Admin: view any product'),
    ('product.manage',      'product', 'manage',      'Admin: manage any product'),
    ('product.review',      'product', 'review',      'Admin: approve or reject product listings'),
    ('product.suspend',     'product', 'suspend',     'Admin: suspend a product'),
    ('product.restore',     'product', 'restore',     'Admin: restore a suspended product')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'product.read.own', 'product.create.own', 'product.update.own', 'product.delete.own',
    'product.publish.own', 'product.archive.own', 'product.manage.own'
)
WHERE r.code = 'SELLER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'product.read', 'product.manage', 'product.review', 'product.suspend', 'product.restore'
)
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

--rollback DELETE FROM permissions WHERE code IN ('product.read.own','product.create.own','product.update.own','product.delete.own','product.publish.own','product.archive.own','product.manage.own','product.read','product.manage','product.review','product.suspend','product.restore');
