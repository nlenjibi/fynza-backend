--liquibase formatted sql

--changeset fynza:031-wishlist-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('wishlist:read',   'WISHLIST', 'READ',    'View own wishlists'),
    ('wishlist:write',  'WISHLIST', 'WRITE',   'Create and update wishlists'),
    ('wishlist:delete', 'WISHLIST', 'DELETE',  'Delete wishlists'),
    ('wishlist:share',  'WISHLIST', 'EXECUTE', 'Share and unshare wishlists')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:031-wishlist-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('wishlist:read', 'wishlist:write', 'wishlist:delete')
WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;
