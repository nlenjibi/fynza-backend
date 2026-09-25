--liquibase formatted sql

--changeset fynza:084-search-permissions splitStatements:false
INSERT INTO permissions (code, description, module, is_active, created_at, updated_at)
VALUES
    ('search.read',              'Search products and stores',          'search', TRUE, NOW(), NOW()),
    ('search.manage',            'Manage search configuration',         'search', TRUE, NOW(), NOW()),
    ('search.analytics.read',    'View search analytics',               'search', TRUE, NOW(), NOW()),
    ('search.index.read',        'View search index status',            'search', TRUE, NOW(), NOW()),
    ('search.index.manage',      'Rebuild and manage search indexes',   'search', TRUE, NOW(), NOW()),
    ('search.dictionary.read',   'View search synonyms',                'search', TRUE, NOW(), NOW()),
    ('search.dictionary.manage', 'Manage search synonyms',              'search', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

