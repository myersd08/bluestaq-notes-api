# Database Migrations

Migrations are managed by [Flyway](https://flywaydb.org/). Scripts live in `src/main/resources/db/migration/` and run automatically on application startup.

---

## Naming Convention

```
V{version}__{description}.sql
```

- `V` — required prefix (uppercase)
- `{version}` — integer, incremented sequentially (`1`, `2`, `3`, …)
- `__` — two underscores separating version from description
- `{description}` — lowercase words separated by underscores

**Examples**
```
V1__create_users_table.sql
V2__create_notes_table.sql
V3__create_shares_table.sql
V4__add_title_to_notes.sql
```

---

## Rules

1. **Never edit a committed migration.** Flyway checksums every applied script — modifying one will cause startup to fail. To fix a mistake, write a new migration.
2. **One concern per file.** A migration that creates a table should not also seed data.
3. **Always include the reverse in a comment.** At the top of each file, add a commented `-- Rollback:` block showing the DROP/ALTER that undoes the migration. Flyway Community doesn't run rollbacks automatically, but the comment preserves the intent.
4. **Test migrations in isolation before committing.** Drop the local DB, run `docker compose up -d`, and verify the app starts cleanly.
5. **Match the JPA entity.** After writing a migration, ensure the corresponding `@Entity` fields align exactly — `ddl-auto: validate` will reject mismatches on startup.

---

## Planned Migrations

| Version | File | Feature | Status |
|---------|------|---------|--------|
| V1 | `V1__create_users_table.sql` | auth | Not started |
| V2 | `V2__create_notes_table.sql` | notes-crud | Not started |
| V3 | `V3__create_shares_table.sql` | note-sharing | Not started |

Update this table when each migration file is created and again when it has been verified running against a clean database.

---

## Adding a New Migration

1. Determine the next version number from the table above.
2. Create `src/main/resources/db/migration/V{n}__{description}.sql`.
3. Write the SQL (see [Schema Reference](#schema-reference) below).
4. Add the entry to the Planned Migrations table in this file.
5. Run `docker compose up -d && ./gradlew bootRun` to verify it applies cleanly.
6. Add a migration task check-off to the relevant feature's `PLAN.md`.

---

## Schema Reference

The canonical data model is in [DOMAIN.md](DOMAIN.md). Migration SQL must match it exactly.

### V1 — users

```sql
-- Rollback: DROP TABLE users;
CREATE TABLE users (
    id          UUID        PRIMARY KEY,
    username    VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

### V2 — notes

```sql
-- Rollback: DROP TABLE notes;
CREATE TABLE notes (
    id          UUID        PRIMARY KEY,
    owner_id    UUID        NOT NULL REFERENCES users(id),
    title       VARCHAR(255),
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

### V3 — shares

```sql
-- Rollback: DROP TABLE shares;
CREATE TABLE shares (
    id                   UUID PRIMARY KEY,
    note_id              UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    shared_with_user_id  UUID NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_share UNIQUE (note_id, shared_with_user_id)
);
```

---

## Configuration

Flyway is configured in `src/main/resources/application.yml`:

```yaml
spring:
  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: false
  jpa:
    hibernate:
      ddl-auto: validate
```

`ddl-auto: validate` means Hibernate will verify the schema matches the entities on startup but will not create or alter any tables — Flyway owns all schema changes.
