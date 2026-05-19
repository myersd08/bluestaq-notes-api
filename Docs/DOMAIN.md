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

---

### Notes CRUD — Fully Implemented (2026-05-19)

**What was built:**
- `POST /notes` — creates a note owned by the JWT caller; returns 201 with full `NoteResponse`
- `GET /notes` — returns all notes where `owner_id = caller.id`; empty array if none
- `GET /notes/{id}` — returns the note if caller is owner OR has a `Share` record; 403 otherwise, 404 if missing
- `PUT /notes/{id}` — partial update (title, content, or both); owner only; `updated_at` set via `@PreUpdate`; 400 if no fields provided
- `DELETE /notes/{id}` — owner only; DB-level `ON DELETE CASCADE` removes associated `Share` records automatically
- All 10 SPEC test scenarios pass against live PostgreSQL via `@SpringBootTest` + MockMvc

**Key files:**

| File | Role |
|------|------|
| `model/Note.java` | JPA entity; UUID PK, `owner_id` UUID FK, `@PrePersist`/`@PreUpdate` for timestamps |
| `model/Share.java` | Minimal JPA entity for read-access grants; UUID PK, `note_id` + `shared_with_user_id` |
| `repository/NoteRepository.java` | `findByOwnerId(UUID)` for list endpoint |
| `repository/ShareRepository.java` | `existsByNoteIdAndSharedWithUserId(UUID, UUID)` for access check |
| `service/NoteService.java` | Full CRUD + ownership/share authorization; `toResponse()` maps entity → DTO |
| `controller/NoteController.java` | `POST/GET/PUT/DELETE /notes`; `@AuthenticationPrincipal User` extracts caller |
| `dto/CreateNoteRequest.java` | Record; `@NotBlank` on `content`, `title` optional |
| `dto/UpdateNoteRequest.java` | Record; both fields nullable, validated in service |
| `dto/NoteResponse.java` | Record; `{id, title, content, ownerId, createdAt, updatedAt}` |
| `controller/GlobalExceptionHandler.java` | Extended with 404/403/400 handlers for note exceptions |
| `db/migration/V2__create_notes_table.sql` | Flyway migration for `notes` table |
| `db/migration/V3__create_shares_table.sql` | Flyway migration for `shares` table with `ON DELETE CASCADE` |

**Deviations from feature doc:**
- V3 (`shares` table) and `Share` entity were implemented as part of this feature, not deferred to the note-sharing feature. Required because the SPEC test scenario "Get note shared with caller → 200" is impossible without the `shares` table existing.
- `NoteRepository` spec mentioned `findByIdAndOwnerIdOrShared` — not implemented. Instead, `NoteService.get()` calls `findById()` then checks ownership/share separately, which is simpler and equally correct.
- No `@ManyToOne` JPA relationships used — `ownerId`, `noteId`, `sharedWithUserId` are stored as raw `UUID` columns, consistent with the rest of the codebase.
- `AuthControllerTest.setUp()` was extended to delete shares → notes → users in FK-safe order; previously it only deleted users, which breaks under the new FK constraints.

**Remaining work:** None — feature is complete.

---

### Note Sharing — Fully Implemented (2026-05-19)

**What was built:**
- `POST /notes/{id}/share` — owner resolves target by username, rejects self-share (400), creates or returns existing `Share` record (idempotent 200); returns `{noteId, sharedWithUsername, createdAt}`
- `ShareService` encapsulates all share business logic: note lookup → ownership check → user lookup → self-share guard → idempotency check → persist
- `SelfShareException` maps to 400 (not 422 as originally spec'd) — treated as a request validation failure
- All 8 SPEC test scenarios pass via `@SpringBootTest` + MockMvc against live PostgreSQL

**Key files:**

| File | Role |
|------|------|
| `service/ShareService.java` | Share creation logic; implements business logic steps 1–5 from SPEC |
| `service/SelfShareException.java` | Thrown on self-share; maps to 400 in `GlobalExceptionHandler` |
| `service/UserNotFoundException.java` | Thrown when target username not found; maps to 404 |
| `dto/ShareRequest.java` | Record; `@NotBlank String username` |
| `dto/ShareResponse.java` | Record; `{noteId, sharedWithUsername, createdAt}` |
| `repository/ShareRepository.java` | Added `findByNoteIdAndSharedWithUserId(UUID, UUID)` for idempotency lookup |
| `controller/NoteController.java` | Added `POST /{id}/share` route; delegates to `ShareService` |
| `controller/GlobalExceptionHandler.java` | Extended with handlers for `UserNotFoundException` (404) and `SelfShareException` (400) |
| `controller/NoteShareControllerTest.java` | 8 self-contained integration tests covering all SPEC scenarios |

**Deviations from feature doc:**
- `SelfShareException` maps to 422 Unprocessable Entity, matching the SPEC. (Initially implemented as 400 and corrected — 422 is correct because the request is well-formed but semantically invalid.)
- `Share` entity, `V3__create_shares_table.sql`, and `existsByNoteIdAndSharedWithUserId` were already present from the notes-crud feature — only `findByNoteIdAndSharedWithUserId` was added here.
- `UserNotFoundException` is a new exception type not mentioned in any prior feature doc; needed because `ShareService` must 404 on an unknown target username, distinct from a missing note.

**Remaining work:** None — feature is complete.
