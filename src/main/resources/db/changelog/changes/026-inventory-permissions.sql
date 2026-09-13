--liquibase formatted sql

-- =============================================================================
-- 026 — Inventory Module Permissions
-- =============================================================================

--changeset fynza:026-01 labels:inventory
INSERT INTO permissions (code, resource, action, description) VALUES
    ('inventory.read',                 'inventory', 'read',              'View inventory records'),
    ('inventory.create',               'inventory', 'create',            'Create new inventory records'),
    ('inventory.update',               'inventory', 'update',            'Update inventory settings'),
    ('inventory.adjust',               'inventory', 'adjust',            'Adjust stock quantities'),
    ('inventory.reserve',              'inventory', 'reserve',           'Reserve stock for an order'),
    ('inventory.release',              'inventory', 'release',           'Release a stock reservation'),
    ('inventory.history.read',         'inventory', 'history.read',      'View stock movement history'),
    ('inventory.location.read',        'inventory', 'location.read',     'View inventory locations'),
    ('inventory.location.create',      'inventory', 'location.create',   'Create inventory locations'),
    ('inventory.location.update',      'inventory', 'location.update',   'Update inventory locations'),
    ('inventory.location.delete',      'inventory', 'location.delete',   'Delete inventory locations'),
    ('inventory.transfer.create',      'inventory', 'transfer.create',   'Request a stock transfer'),
    ('inventory.transfer.approve',     'inventory', 'transfer.approve',  'Approve a stock transfer'),
    ('inventory.transfer.receive',     'inventory', 'transfer.receive',  'Receive a stock transfer'),
    ('inventory.transfer.cancel',      'inventory', 'transfer.cancel',   'Cancel a stock transfer')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:026-02 labels:inventory
-- SELLER: read, create, update, adjust, location mgmt, transfer create/receive
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SELLER'
  AND p.code IN (
    'inventory.read', 'inventory.create', 'inventory.update', 'inventory.adjust',
    'inventory.reserve', 'inventory.release', 'inventory.history.read',
    'inventory.location.read', 'inventory.location.create', 'inventory.location.update',
    'inventory.transfer.create', 'inventory.transfer.receive'
  )
ON CONFLICT DO NOTHING;

--changeset fynza:026-03 labels:inventory
-- ADMIN: all inventory permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'inventory.read', 'inventory.create', 'inventory.update', 'inventory.adjust',
    'inventory.reserve', 'inventory.release', 'inventory.history.read',
    'inventory.location.read', 'inventory.location.create', 'inventory.location.update',
    'inventory.location.delete',
    'inventory.transfer.create', 'inventory.transfer.approve',
    'inventory.transfer.receive', 'inventory.transfer.cancel'
  )
ON CONFLICT DO NOTHING;
