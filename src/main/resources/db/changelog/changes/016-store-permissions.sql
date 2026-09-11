--liquibase formatted sql
--changeset fynza:016-store-permissions dbms:postgresql

INSERT INTO permissions (code, resource, action, description) VALUES
    ('store.read.own',    'store', 'read.own',    'View own store(s)'),
    ('store.create.own',  'store', 'create.own',  'Create a store for own seller account'),
    ('store.update.own',  'store', 'update.own',  'Update own store profile'),
    ('store.delete.own',  'store', 'delete.own',  'Soft-delete own store'),
    ('store.publish.own', 'store', 'publish.own', 'Publish own store (DRAFT → ACTIVE)'),
    ('store.pause.own',   'store', 'pause.own',   'Pause own active store'),
    ('store.manage.own',  'store', 'manage.own',  'Full self-service store management'),
    ('store.read',        'store', 'read',        'Admin: view any store'),
    ('store.suspend',     'store', 'suspend',     'Admin: suspend a store'),
    ('store.activate',    'store', 'activate',    'Admin: reactivate a suspended store'),
    ('store.close',       'store', 'close',       'Admin: permanently close a store')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'store.read.own', 'store.create.own', 'store.update.own', 'store.delete.own',
    'store.publish.own', 'store.pause.own', 'store.manage.own'
)
WHERE r.code = 'SELLER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'store.read', 'store.suspend', 'store.activate', 'store.close'
)
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

--rollback DELETE FROM permissions WHERE code IN ('store.read.own','store.create.own','store.update.own','store.delete.own','store.publish.own','store.pause.own','store.manage.own','store.read','store.suspend','store.activate','store.close');
