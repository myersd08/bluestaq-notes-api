-- Rollback: DROP TABLE notes;
CREATE TABLE notes (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL REFERENCES users(id),
    title       VARCHAR(255),
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
