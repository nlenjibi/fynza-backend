--liquibase formatted sql

--changeset fynza:005-linked-identities
CREATE TABLE linked_identities (
    id               UUID         NOT NULL PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider         VARCHAR(50)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    display_name     VARCHAR(255),
    avatar_url       VARCHAR(500),
    linked_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_linked_identity_provider UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_linked_identities_user ON linked_identities(user_id);
CREATE INDEX idx_linked_identities_provider ON linked_identities(provider);

--rollback DROP TABLE IF EXISTS linked_identities;
