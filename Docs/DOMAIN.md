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
- **`ddl-auto: validate`** in all environments — Flyway owns schema changes; Hibernate only validates that entities match the migrated schema on startup.
- **No soft deletes** — notes are hard-deleted; out of scope for this project.
- **Title is optional** — the spec only requires `content`; title improves list usability.

---

## Implemented Features

### Authentication — Fully Implemented (2026-05-19)

**What was built:**
- `POST /auth/register` — BCrypt-hashes password, persists `User`, returns 201 with `{id, username, createdAt}`
- `POST /auth/login` — verifies BCrypt hash, returns 200 with `{token, expiresIn}`
- `JwtAuthFilter` validates every non-`/auth/**` request; populates `SecurityContext` from JWT `sub` (user UUID)
- All 7 SPEC test scenarios pass against live PostgreSQL via `@SpringBootTest` + MockMvc

**Key files:**

| File | Role |
|------|------|
| `model/User.java` | JPA entity + `UserDetails` impl; UUID PK, BCrypt `password_hash` |
| `security/JwtTokenService.java` | Generate / validate / parse JWT (jjwt 0.12.6); HS256, key from UTF-8 secret bytes |
| `security/JwtAuthFilter.java` | `OncePerRequestFilter`; extracts Bearer token, loads user by UUID |
| `config/SecurityConfig.java` | Stateless, CSRF off, `/auth/**` open, custom 401 entry point |
| `service/AuthService.java` | Register + login business logic |
| `controller/AuthController.java` | `POST /auth/register`, `POST /auth/login` |
| `controller/GlobalExceptionHandler.java` | `{status, error, message}` envelope for 400 / 401 / 409 |
| `db/migration/V1__create_users_table.sql` | Flyway migration for `users` table |

**Deviations from feature doc:**
- `User` implements `UserDetails` directly (spec implied a separate `UserDetailsService` wrapper — both exist, but the entity carries the interface too)
- `JwtAuthFilter` uses `UserRepository.findById(UUID)` rather than `UserDetailsService.loadUserByUsername` — JWT `sub` is a UUID, making a username lookup unnecessary
- `spring-boot-flyway` added to `build.gradle.kts` — required in Spring Boot 4 (not documented in original setup)
- Docker PostgreSQL moved to port 5434 to avoid conflict with a native install on 5432

**Remaining work:** None — feature is complete.
