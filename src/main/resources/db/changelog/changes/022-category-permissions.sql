--liquibase formatted sql

--changeset fynza:022-01 labels:category
INSERT INTO permissions (code, resource, action, description) VALUES
    ('category.read',               'category', 'read',               'View categories'),
    ('category.create',             'category', 'create',             'Create categories'),
    ('category.update',             'category', 'update',             'Update categories'),
    ('category.delete',             'category', 'delete',             'Delete categories'),
    ('category.move',               'category', 'move',               'Move categories in hierarchy'),
    ('category.publish',            'category', 'publish',            'Activate/deactivate categories'),
    ('category.archive',            'category', 'archive',            'Archive categories'),
    ('category.attribute.manage',   'category', 'attribute.manage',   'Manage category attribute definitions'),
    ('category.suggestion.create',  'category', 'suggestion.create',  'Submit category suggestions'),
    ('category.suggestion.review',  'category', 'suggestion.review',  'Approve or reject category suggestions')
ON CONFLICT (code) DO NOTHING;

--changeset fynza:022-02 labels:category
-- SELLER: read + suggest
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SELLER'
  AND p.code IN ('category.read', 'category.suggestion.create')
ON CONFLICT DO NOTHING;

--changeset fynza:022-03 labels:category
-- ADMIN: all category permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'category.read', 'category.create', 'category.update', 'category.delete',
    'category.move', 'category.publish', 'category.archive',
    'category.attribute.manage', 'category.suggestion.create', 'category.suggestion.review'
  )
ON CONFLICT DO NOTHING;
