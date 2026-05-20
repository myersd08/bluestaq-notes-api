# Secure Notes Vault API

A REST API for creating, managing, and sharing notes with JWT authentication and per-note access control.

---

## Architecture

```
Client
  └── HTTP/REST
       └── Spring Boot 4 (Java 17)
            ├── Spring Security  →  JwtAuthFilter  →  SecurityContext
            ├── Controllers  →  Services  →  Repositories
            └── Spring Data JPA / Hibernate  →  PostgreSQL 16
```

**Package structure**

```
com.bluestaq.notesapi
├── config/        SecurityConfig — filter chain, password encoder
├── controller/    AuthController, NoteController, GlobalExceptionHandler
├── dto/           Request/response Java records (no JPA annotations)
├── model/         JPA entities: User, Note, Share
├── repository/    Spring Data JPA repositories
├── security/      JwtTokenService, JwtAuthFilter, UserDetailsServiceImpl
└── service/       AuthService, NoteService, ShareService
```

---

## Tech Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | Java 17 | LTS release; strong Spring ecosystem; required by Spring Boot 4 |
| Framework | Spring Boot 4.0.6 | Industry standard for Java REST APIs; batteries-included security, validation, JPA |
| Database | PostgreSQL 16 (Docker) | Relational model naturally fits the owner/share access pattern; Docker removes local-install friction |
| Migrations | Flyway 11 | Schema changes are versioned, auditable, and applied automatically on startup — `ddl-auto: validate` keeps Hibernate honest |
| Auth | JWT via jjwt 0.12.6 | Stateless; no server-side session store; straightforward to test |
| ORM | Spring Data JPA / Hibernate 7 | Reduces CRUD boilerplate; UUID primary keys are first-class in JPA 3.1 |
| Build | Gradle 8 (Kotlin DSL) | Faster incremental builds than Maven; type-safe DSL |
| API Docs | springdoc-openapi 3.0.3 + Swagger UI | Generates OpenAPI 3 spec from annotations; interactive UI with JWT auth support |
| Utilities | Lombok | Reduces boilerplate (getters, constructors) without changing runtime behaviour |

---

## Prerequisites

- Java 17+
- Docker & Docker Compose

No other local installations required. PostgreSQL runs entirely inside Docker.

---

## Running Locally

**1. Start the database**

```bash
docker compose up -d
```

Starts PostgreSQL 16 on **`localhost:5434`** (port 5434 to avoid conflicts with any local PostgreSQL install) with:
- Database: `notes_db`
- User: `notes_user`
- Password: `notes_pass`

Flyway migrations run automatically on application startup; the schema is created fresh on first boot.

**2. Start the application**

```bash
./gradlew bootRun
```

The API is available at **`http://localhost:8081`**.

Once running, the interactive API documentation is available at:
- **Swagger UI:** `http://localhost:8081/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

> If port 8081 is already in use (e.g., a previous test run left a JVM running), free it first:
> ```powershell
> # PowerShell
> Get-NetTCPConnection -LocalPort 8081 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
> ```

**3. Override the JWT secret (recommended for any shared environment)**

```bash
JWT_SECRET=your-secret-at-least-32-chars ./gradlew bootRun
```

The default secret in `application.yml` is for local development only. Set `JWT_SECRET` in any non-local environment.

---

## Running Tests

```bash
docker compose up -d   # must be running before tests
./gradlew test
```

Tests use `@SpringBootTest` + MockMvc against a live PostgreSQL database — there is no H2 fallback. Each test class clears its own data in `@BeforeEach`.

---

## Implementation Status

| Feature | Status | Notes |
|---------|--------|-------|
| Authentication (`/auth/**`) | ✅ Complete | All 7 test scenarios pass |
| Notes CRUD (`/notes/**`) | ✅ Complete | All 10 test scenarios pass |
| Note Sharing (`/notes/{id}/share`) | ✅ Complete | All 8 test scenarios pass |

---

## API Reference

All protected endpoints require:
```
Authorization: Bearer <token>
```

Errors follow a consistent envelope:
```json
{ "status": 409, "error": "Conflict", "message": "Username already taken" }
```

### Authentication — ✅ Implemented

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | No | Create a new account |
| POST | `/auth/login` | No | Receive a JWT |

### Notes CRUD — ✅ Implemented

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/notes` | Yes | Create a note |
| GET | `/notes` | Yes | List caller's notes |
| GET | `/notes/{id}` | Yes | Get note (owner or shared recipient) |
| PUT | `/notes/{id}` | Yes — owner only | Update title/content |
| DELETE | `/notes/{id}` | Yes — owner only | Hard delete (cascades shares) |
| POST | `/notes/{id}/share` | Yes — owner only | Grant read access to another user (idempotent) |

---

## Example Usage

A realistic flow from registration to sharing a note.

**Register**
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "s3cur3P@ss"}'
# 201 → { "id": "...", "username": "alice", "createdAt": "..." }
```

**Login** (save the token for subsequent requests)
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "s3cur3P@ss"}'
# 200 → { "token": "<jwt>", "expiresIn": 86400000 }
```

**Create a note**
```bash
curl -X POST http://localhost:8081/notes \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title": "Shopping list", "content": "Milk, eggs, bread"}'
# 201 → { "id": "<note-id>", "title": "Shopping list", "content": "...", "ownerId": "...", "createdAt": "...", "updatedAt": "..." }
```

**List your notes**
```bash
curl http://localhost:8081/notes \
  -H "Authorization: Bearer <token>"
# 200 → [ { "id": "...", ... }, ... ]
```

**Get a single note**
```bash
curl http://localhost:8081/notes/<note-id> \
  -H "Authorization: Bearer <token>"
# 200 → note object; 403 if not owner or shared recipient; 404 if not found
```

**Update a note** (owner only; supply title, content, or both)
```bash
curl -X PUT http://localhost:8081/notes/<note-id> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"content": "Milk, eggs, bread, butter"}'
# 200 → updated note; 403 if not owner; 404 if not found
```

**Delete a note** (owner only; cascades to all shares)
```bash
curl -X DELETE http://localhost:8081/notes/<note-id> \
  -H "Authorization: Bearer <token>"
# 204 No Content; 403 if not owner; 404 if not found
```

**Register bob** (the share recipient)
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "bob", "password": "s3cur3P@ss"}'
# 201 → { "id": "...", "username": "bob", "createdAt": "..." }
```

**Share a note with bob** (owner only; idempotent — repeat calls return 200 without creating a duplicate)
```bash
curl -X POST http://localhost:8081/notes/<note-id>/share \
  -H "Authorization: Bearer <alice-token>" \
  -H "Content-Type: application/json" \
  -d '{"username": "bob"}'
# 200 → { "noteId": "<note-id>", "sharedWithUsername": "bob", "createdAt": "..." }
# 403 if caller is not the owner; 404 if note or username not found; 422 if sharing with yourself
```

**Access the shared note as bob**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "bob", "password": "s3cur3P@ss"}' | jq -r .token)

curl http://localhost:8081/notes/<note-id> \
  -H "Authorization: Bearer $TOKEN"
# 200 → note content (read-only; PUT and DELETE return 403)
```

---

## Security Considerations

- **Passwords** are hashed with BCrypt before storage. Plaintext passwords are never persisted or logged.
- **JWT tokens** are signed with HMAC-SHA256 and expire after 24 hours (configurable via `app.jwt.expiration-ms`). The `sub` claim holds the user's UUID, not their username.
- **Authorization** is enforced at the service layer. Update, delete, and share operations require the caller to be the note owner; read access is granted to the owner or any explicitly-shared recipient.
- **Error messages** on login are intentionally generic ("Invalid username or password") to avoid confirming whether a username exists.
- **Out of scope:** HTTPS termination (handled by a reverse proxy in production), rate limiting, refresh tokens, account lockout.

---

## Assumptions & Tradeoffs

- **Flyway owns the schema.** `ddl-auto: validate` is set in `application.yml` — Hibernate validates entities against the migrated schema but never alters tables. All schema changes go through numbered migration scripts in `src/main/resources/db/migration/`.
- **Hard deletes.** Deleting a note permanently removes it and cascades to all associated `Share` records. Soft deletes were out of scope.
- **No email or account verification.** Username is the only identity. An email field and verification flow would be required for production.
- **Sharing is idempotent.** Sharing a note with a user who already has access returns 200 without creating a duplicate record.
- **UUID primary keys.** Avoids leaking sequential IDs and simplifies any future distributed deployment.
- **Port 5434 for PostgreSQL.** The Docker container is mapped to 5434 (not the standard 5432) to avoid conflict with a locally-installed PostgreSQL instance. If your machine has no local PostgreSQL, you can change this back to 5432 in `docker-compose.yml` and `application.yml`.

---

## Future Improvements

- Refresh token support with rotation
- Pagination on `GET /notes`
- Paginated `GET /notes/shared` for getting notes that have been shared
- Add `DELETE /notes/{id}/share` for removing a share
- Full-text search on note content
- Audit log (who accessed or modified a note and when)
- Rate limiting on auth endpoints
- `@NotBlank` validation on `LoginRequest` fields (currently returns 401 rather than 400 for missing username — intentional but worth reconsidering)
- Concurrency capability for shared notes that allows the note to be edited by the owner and shared recipient

---

## Design Considerations

<details>
<summary>Production deployment</summary>

Containerise the application with a `Dockerfile` and deploy via a container orchestrator (Kubernetes or AWS ECS). PostgreSQL moves to a managed service (RDS, Cloud SQL). Environment-specific config — DB URL, `JWT_SECRET`, etc. — is injected via environment variables or a secrets manager (AWS Secrets Manager, Vault). A reverse proxy (nginx, ALB) handles TLS termination. Flyway migrations run as an init container or a pre-deploy hook, not during the app's startup path.

</details>

<details>
<summary>Observability</summary>

Expose Spring Boot Actuator health and metrics endpoints and scrape with Prometheus/Grafana. Key signals to alert on: HTTP 5xx error rate, p99 request latency, DB connection pool saturation (`hikaricp_connections_active`), and JWT validation failure rate (a sudden spike suggests credential scanning). Structured JSON logging with a correlation ID per request aids distributed tracing. Integrate with OpenTelemetry for trace propagation across services.

</details>

<details>
<summary>Database migrations</summary>

Flyway is already in use. Migration scripts live in `src/main/resources/db/migration/` and run automatically on startup. Each script is versioned (`V1__create_users_table.sql`, `V2__create_notes_table.sql`, …) and checksummed — editing an applied migration causes startup to fail, enforcing an append-only migration history. For destructive changes (column drops, type changes) the pattern is: add the new column in migration N, backfill in migration N+1, drop the old column in migration N+2 — never in a single migration.

</details>

<details>
<summary>Scaling to 10,000 concurrent users</summary>

The stateless JWT design means any number of app instances can run behind a load balancer without session affinity. The primary bottleneck at scale is the database. A read replica handles `GET /notes` traffic while the primary handles writes. HikariCP (already bundled by Spring Boot) is tuned to match the DB's `max_connections`. If per-note access-control checks (`SELECT` from `shares` on every `GET /notes/{id}`) become a hot path, a denormalised access-control table or a short-lived Redis cache of `(user_id, note_id)` grants reduces query volume significantly.

</details>
