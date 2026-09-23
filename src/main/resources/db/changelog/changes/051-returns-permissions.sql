--liquibase formatted sql

--changeset fynza:051-returns-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('return:read',        'RETURN', 'READ',    'View own return requests'),
    ('return:create',      'RETURN', 'WRITE',   'Submit a return request'),
    ('return:cancel',      'RETURN', 'WRITE',   'Cancel own return request'),
    ('return:review',      'RETURN', 'EXECUTE', 'Review and manage return requests (seller/support)'),
    ('return:approve',     'RETURN', 'EXECUTE', 'Approve a return request'),
    ('return:reject',      'RETURN', 'EXECUTE', 'Reject a return request'),
    ('return:inspect',     'RETURN', 'EXECUTE', 'Inspect returned items'),
    ('return:resolve',     'RETURN', 'EXECUTE', 'Resolve a return case'),
    ('return:admin.read',   'RETURN', 'READ',    'Admin read access to all returns'),
    ('return:admin.manage', 'RETURN', 'EXECUTE', 'Full admin management of returns'),
    ('return:policy.read',  'RETURN', 'READ',    'View return policies'),
    ('return:policy.manage','RETURN', 'EXECUTE', 'Create and manage return policies')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:051-returns-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('return:read', 'return:create', 'return:cancel')
WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('return:read', 'return:review', 'return:approve', 'return:reject', 'return:inspect', 'return:resolve')
WHERE r.code = 'SELLER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'return:read', 'return:review', 'return:approve', 'return:reject',
    'return:inspect', 'return:resolve', 'return:admin.read', 'return:admin.manage',
    'return:policy.read', 'return:policy.manage'
)
WHERE r.code IN ('ADMIN', 'MANAGER')
ON CONFLICT DO NOTHING;
