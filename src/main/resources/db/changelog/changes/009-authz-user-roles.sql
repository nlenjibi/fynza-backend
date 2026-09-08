--liquibase formatted sql
--changeset fynza:009-authz-user-roles dbms:postgresql
CREATE TABLE IF NOT EXISTS user_roles (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    role_id     UUID         NOT NULL REFERENCES roles(id),
    scope_type  VARCHAR(50)  NOT NULL DEFAULT 'GLOBAL',
    scope_id    UUID,
    assigned_by UUID,
    assigned_at TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_active ON user_roles (user_id, active);
CREATE INDEX IF NOT EXISTS idx_user_roles_expires     ON user_roles (expires_at) WHERE expires_at IS NOT NULL;
INSERT INTO user_roles (user_id, role_id, scope_type, active)
SELECT u.id, r.id, 'GLOBAL', TRUE
FROM users u JOIN roles r ON r.code = u.role
ON CONFLICT DO NOTHING;
--rollback DROP TABLE IF EXISTS user_roles;
