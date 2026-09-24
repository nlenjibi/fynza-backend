--liquibase formatted sql

--changeset fynza:064-review-permissions
INSERT INTO permissions (code, resource, action, description) VALUES
    ('review:read',           'REVIEW', 'READ',    'Read public reviews'),
    ('review:create',         'REVIEW', 'WRITE',   'Submit a product review'),
    ('review:update',         'REVIEW', 'WRITE',   'Edit own review'),
    ('review:delete',         'REVIEW', 'WRITE',   'Delete own review'),
    ('review:vote',           'REVIEW', 'WRITE',   'Vote a review as helpful or not helpful'),
    ('review:report',         'REVIEW', 'WRITE',   'Report a review for moderation'),
    ('review:respond',        'REVIEW', 'EXECUTE', 'Post a seller response to a review'),
    ('review:moderate',       'REVIEW', 'EXECUTE', 'Moderate reviews (approve, reject, hide, restore)'),
    ('review:admin.read',     'REVIEW', 'READ',    'Admin read access to all reviews'),
    ('review:admin.manage',   'REVIEW', 'EXECUTE', 'Full admin management of reviews and moderation')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:064-review-role-grants
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('review:read', 'review:create', 'review:update', 'review:delete', 'review:vote', 'review:report')
WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('review:read', 'review:respond')
WHERE r.code = 'SELLER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'review:read', 'review:moderate', 'review:admin.read', 'review:admin.manage'
)
WHERE r.code IN ('ADMIN', 'MANAGER')
ON CONFLICT DO NOTHING;
