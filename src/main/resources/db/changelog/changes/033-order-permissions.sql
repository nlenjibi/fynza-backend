--liquibase formatted sql

--changeset fynza:033-order-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('order:read',   'ORDER', 'READ',    'View own orders'),
    ('order:write',  'ORDER', 'WRITE',   'Create and cancel orders'),
    ('order:admin',  'ORDER', 'EXECUTE', 'Admin order management')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:033-order-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('order:read', 'order:write')
WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('order:read', 'order:write')
WHERE r.code = 'SELLER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('order:read', 'order:write', 'order:admin')
WHERE r.code IN ('ADMIN', 'MANAGER')
ON CONFLICT DO NOTHING;
