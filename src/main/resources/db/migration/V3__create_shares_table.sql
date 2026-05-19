-- Rollback: DROP TABLE shares;
CREATE TABLE shares (
    id                   UUID        PRIMARY KEY,
    note_id              UUID        NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    shared_with_user_id  UUID        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_share UNIQUE (note_id, shared_with_user_id)
);
