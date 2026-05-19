# Domain Model — Secure Notes Vault API

## Entities

### User
Represents an authenticated account in the system.

| Field           | Type        | Notes                        |
|-----------------|-------------|------------------------------|
| `id`            | UUID        | Primary key, generated       |
| `username`      | String      | Unique, not null             |
| `password_hash` | String      | BCrypt hashed, never exposed |
| `created_at`    | Instant     | Set on insert, immutable     |

### Note
A piece of content owned by a user.

| Field        | Type    | Notes                          |
|--------------|---------|--------------------------------|
| `id`         | UUID    | Primary key, generated         |
| `owner_id`   | UUID    | FK → User, not null            |
| `title`      | String  | Optional, max 255 chars        |
| `content`    | String  | The note body, not null        |
| `created_at` | Instant | Set on insert, immutable       |
| `updated_at` | Instant | Updated on every write         |

### Share
A grant giving another user read-only access to a specific note.

| Field                | Type    | Notes                              |
|----------------------|---------|------------------------------------|
| `id`                 | UUID    | Primary key, generated             |
| `note_id`            | UUID    | FK → Note, not null                |
| `shared_with_user_id`| UUID    | FK → User, not null                |
| `created_at`         | Instant | Set on insert, immutable           |

Unique constraint on `(note_id, shared_with_user_id)` — a note can only be shared once per user.

---

## Relationships

```
User ──< Note         (one user owns many notes)
Note ──< Share        (one note can be shared with many users)
User ──< Share        (one user can receive many shared notes)
```

---

## Business Rules

1. **Ownership** — Only the note owner may update, delete, or share a note.
2. **Read access** — A note is readable by its owner OR any user it has been shared with.
3. **Share is read-only** — Recipients of a shared note cannot modify or re-share it.
4. **No self-share** — A user cannot share a note with themselves.
5. **No duplicate share** — Sharing the same note with the same user twice is idempotent (return 200, not error).
6. **Password storage** — Passwords are never stored in plaintext; BCrypt is the hashing algorithm.
7. **Token expiry** — JWT tokens expire after 24 hours (configurable via `app.jwt.expiration-ms`).

---

## Package Structure

```
com.bluestaq.notesapi
├── config/        Spring Security config, JWT bean wiring
├── controller/    REST controllers (AuthController, NoteController)
├── dto/           Request/response record classes (no JPA annotations)
├── model/         JPA entities (User, Note, Share)
├── repository/    Spring Data JPA repositories
├── security/      JWT filter, token service, UserDetails impl
└── service/       Business logic (AuthService, NoteService, ShareService)
```

---

## Key Design Decisions

- **UUID primary keys** — avoids leaking sequential IDs and simplifies distributed scenarios.
- **`ddl-auto: update`** for local dev — acceptable for a take-home project; production would use Flyway/Liquibase migrations.
- **No soft deletes** — notes are hard-deleted; out of scope for this project.
- **Title is optional** — the spec only requires `content`; title improves list usability.
