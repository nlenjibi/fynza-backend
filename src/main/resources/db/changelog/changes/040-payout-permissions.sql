--liquibase formatted sql

--changeset fynza:040-payout-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('payout:read',          'PAYOUT', 'READ',    'View own payout history and wallet'),
    ('payout:write',         'PAYOUT', 'WRITE',   'Request and cancel payouts'),
    ('payout:admin',         'PAYOUT', 'EXECUTE', 'Admin payout management'),
    ('payout:account:write', 'PAYOUT', 'WRITE',   'Manage payout accounts')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:040-payout-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('payout:read', 'payout:write', 'payout:account:write')
WHERE r.code = 'SELLER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('payout:read', 'payout:write', 'payout:admin', 'payout:account:write')
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
