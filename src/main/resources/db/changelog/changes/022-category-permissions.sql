--liquibase formatted sql

--changeset fynza:022-01 labels:category
INSERT INTO permissions (public_id, name, description) VALUES
    (gen_random_uuid(), 'category.read',               'View categories'),
    (gen_random_uuid(), 'category.create',             'Create categories'),
    (gen_random_uuid(), 'category.update',             'Update categories'),
    (gen_random_uuid(), 'category.delete',             'Delete categories'),
    (gen_random_uuid(), 'category.move',               'Move categories in hierarchy'),
    (gen_random_uuid(), 'category.publish',            'Activate/deactivate categories'),
    (gen_random_uuid(), 'category.archive',            'Archive categories'),
    (gen_random_uuid(), 'category.attribute.manage',   'Manage category attribute definitions'),
    (gen_random_uuid(), 'category.suggestion.create',  'Submit category suggestions'),
    (gen_random_uuid(), 'category.suggestion.review',  'Approve or reject category suggestions')
ON CONFLICT (name) DO NOTHING;

--changeset fynza:022-02 labels:category
-- SELLER: read + suggest
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SELLER'
  AND p.name IN ('category.read', 'category.suggestion.create')
ON CONFLICT DO NOTHING;

--changeset fynza:022-03 labels:category
-- ADMIN: all category permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
    'category.read', 'category.create', 'category.update', 'category.delete',
    'category.move', 'category.publish', 'category.archive',
    'category.attribute.manage', 'category.suggestion.create', 'category.suggestion.review'
  )
ON CONFLICT DO NOTHING;
