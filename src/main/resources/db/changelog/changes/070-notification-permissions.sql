--liquibase formatted sql

--changeset fynza:070-notification-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('notification:read',               'NOTIFICATION', 'READ',    'Read own in-app notifications'),
    ('notification:manage',             'NOTIFICATION', 'EXECUTE', 'Manage own notifications (mark read, delete)'),
    ('notification.preference:read',    'NOTIFICATION', 'READ',    'Read own notification preferences'),
    ('notification.preference:update',  'NOTIFICATION', 'WRITE',   'Update own notification preferences'),
    ('notification.template:read',      'NOTIFICATION', 'READ',    'Admin read access to notification templates'),
    ('notification.template:create',    'NOTIFICATION', 'WRITE',   'Admin create notification templates'),
    ('notification.template:update',    'NOTIFICATION', 'WRITE',   'Admin update notification templates'),
    ('notification.template:delete',    'NOTIFICATION', 'WRITE',   'Admin delete notification templates'),
    ('notification.delivery:read',      'NOTIFICATION', 'READ',    'Admin read notification delivery records'),
    ('notification.delivery:retry',     'NOTIFICATION', 'EXECUTE', 'Admin retry failed notification dispatches'),
    ('notification.admin:read',         'NOTIFICATION', 'READ',    'Full admin read access to all notifications'),
    ('notification.admin:manage',       'NOTIFICATION', 'EXECUTE', 'Full admin management of notifications')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:070-notification-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('notification:read', 'notification:manage',
                                  'notification.preference:read', 'notification.preference:update')
WHERE r.code IN ('CUSTOMER', 'SELLER')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'notification:read', 'notification:manage',
    'notification.preference:read', 'notification.preference:update',
    'notification.template:read', 'notification.template:create', 'notification.template:update',
    'notification.delivery:read', 'notification.delivery:retry',
    'notification.admin:read', 'notification.admin:manage'
)
WHERE r.code IN ('ADMIN', 'MANAGER')
ON CONFLICT DO NOTHING;
