--liquibase formatted sql

-- =============================================================================
-- 028 — Media Module Permissions
-- =============================================================================

--changeset fynza:028-01 labels:media
INSERT INTO permissions (code, resource, action, description) VALUES
    ('media.upload',              'media', 'upload',              'Initiate a media upload session'),
    ('media.read',                'media', 'read',                'View media asset metadata'),
    ('media.delete',              'media', 'delete',              'Delete a media asset'),
    ('media.delete.any',          'media', 'delete.any',          'Delete any media asset (admin)'),
    ('media.url.read',            'media', 'url.read',            'Generate signed download URL for private media'),
    ('media.quota.read',          'media', 'quota.read',          'View own storage quota and usage'),
    ('media.quota.manage',        'media', 'quota.manage',        'Manage storage quotas (admin)'),
    ('media.usage.read',          'media', 'usage.read',          'View storage usage metrics (admin)'),
    ('media.session.cancel',      'media', 'session.cancel',      'Cancel an in-progress upload session'),
    ('media.product.attach',      'media', 'product.attach',      'Attach a media asset to a product'),
    ('media.product.detach',      'media', 'product.detach',      'Detach a media asset from a product'),
    ('media.product.reorder',     'media', 'product.reorder',     'Reorder product media assets')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:028-02 labels:media
-- SELLER: upload, read own, delete own, signed URLs, quota view, session cancel, product media mgmt
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SELLER'
  AND p.code IN (
    'media.upload', 'media.read', 'media.delete', 'media.url.read',
    'media.quota.read', 'media.session.cancel',
    'media.product.attach', 'media.product.detach', 'media.product.reorder'
  )
ON CONFLICT DO NOTHING;

--changeset fynza:028-03 labels:media
-- ADMIN: all media permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'media.upload', 'media.read', 'media.delete', 'media.delete.any',
    'media.url.read', 'media.quota.read', 'media.quota.manage',
    'media.usage.read', 'media.session.cancel',
    'media.product.attach', 'media.product.detach', 'media.product.reorder'
  )
ON CONFLICT DO NOTHING;
