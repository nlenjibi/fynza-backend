--liquibase formatted sql

-- =============================================================================
-- 024 — Pricing Module Permissions
-- =============================================================================

--changeset fynza:024-01 labels:pricing
INSERT INTO permissions (code, resource, action, description) VALUES
    ('price.read',         'price', 'read',         'View prices and price history'),
    ('price.create',       'price', 'create',       'Create new prices'),
    ('price.update',       'price', 'update',       'Update existing prices'),
    ('price.delete',       'price', 'delete',       'Delete/deactivate prices'),
    ('price.activate',     'price', 'activate',     'Activate a draft or scheduled price'),
    ('price.disable',      'price', 'disable',      'Disable an active price'),
    ('price.schedule',     'price', 'schedule',     'Schedule a future price'),
    ('price.history.read', 'price', 'history.read', 'View price change history'),
    ('price.override',     'price', 'override',     'Apply an administrative price override'),
    ('price.bulk_import',  'price', 'bulk_import',  'Bulk-import prices via CSV/API')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:024-02 labels:pricing
-- SELLER: create, read, update, activate, disable, schedule, history
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SELLER'
  AND p.code IN (
    'price.read', 'price.create', 'price.update',
    'price.activate', 'price.disable', 'price.schedule',
    'price.history.read'
  )
ON CONFLICT DO NOTHING;

--changeset fynza:024-03 labels:pricing
-- ADMIN: all pricing permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'price.read', 'price.create', 'price.update', 'price.delete',
    'price.activate', 'price.disable', 'price.schedule',
    'price.history.read', 'price.override', 'price.bulk_import'
  )
ON CONFLICT DO NOTHING;
