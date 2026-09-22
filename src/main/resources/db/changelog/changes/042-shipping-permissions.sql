--liquibase formatted sql

--changeset fynza:042-shipping-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('shipping:read',   'SHIPPING', 'READ',    'View own shipments and tracking'),
    ('shipping:write',  'SHIPPING', 'WRITE',   'Create and manage shipments'),
    ('shipping:admin',  'SHIPPING', 'EXECUTE', 'Admin shipping management (carriers, methods, zones, rates)')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:042-shipping-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('shipping:read')
WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('shipping:read', 'shipping:write')
WHERE r.code = 'SELLER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('shipping:read', 'shipping:write', 'shipping:admin')
WHERE r.code IN ('ADMIN', 'MANAGER')
ON CONFLICT DO NOTHING;
