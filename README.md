# Secure Notes Vault API

A REST API for creating, managing, and sharing notes with authentication and access control.

---

## Architecture

```
Client
  └── HTTP/REST
       └── Spring Boot (Java 17)
            ├── Spring Security + JWT filter
            ├── Controllers  →  Services  →  Repositories
            └── Spring Data JPA  →  PostgreSQL
```

**Package structure**

```
com.bluestaq.notesapi
├── config/        Spring Security config, bean wiring
├── controller/    REST controllers (AuthController, NoteController)
├── dto/           Request/response record classes
├── model/         JPA entities (User, Note, Share)
├── repository/    Spring Data JPA repositories
├── security/      JWT filter, token service, UserDetails impl
└── service/       Business logic (AuthService, NoteService, ShareService)
```

---

## Tech Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | Java 17 | LTS release, widely deployed, strong Spring ecosystem |
| Framework | Spring Boot 4.0.6 | Industry standard for Java REST APIs; batteries included for security, validation, JPA |
| Database | PostgreSQL 16 (Docker) | Relational model fits the owner/share access pattern; Docker removes local install friction |
| Auth | JWT via jjwt 0.12.6 | Stateless, no server-side session store needed; straightforward to implement and test |
| ORM | Spring Data JPA / Hibernate | Reduces boilerplate for CRUD; `ddl-auto: update` is acceptable for a local take-home project |
| Build | Gradle 8 (Kotlin DSL) | Faster incremental builds than Maven; type-safe DSL |
| Utilities | Lombok | Reduces boilerplate (getters, constructors, builders) without changing runtime behaviour |

---

## Prerequisites

- Java 17+
- Docker & Docker Compose

---

## Running Locally

**1. Start the database**

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with database `notes_db`, user `notes_user`, password `notes_pass`.

**2. Start the application**

```bash
./gradlew bootRun
```

The API is available at `http://localhost:8080`.

**3. (Optional) Override the JWT secret**

```bash
JWT_SECRET=your-256-bit-secret ./gradlew bootRun
```

> The default secret in `application.yml` is for local development only. Always set `JWT_SECRET` in any shared or production environment.

---

## Running Tests

```bash
./gradlew test
```

---

## API Reference

All endpoints that require authentication expect the header:
```
Authorization: Bearer <token>
```

### Authentication

| Method | Endpoint | Auth required |
|--------|----------|---------------|
| POST | `/auth/register` | No |
| POST | `/auth/login` | No |

### Notes

| Method | Endpoint | Auth required |
|--------|----------|---------------|
| POST | `/notes` | Yes |
| GET | `/notes` | Yes |
| GET | `/notes/{id}` | Yes |
| PUT | `/notes/{id}` | Yes (owner only) |
| DELETE | `/notes/{id}` | Yes (owner only) |
| POST | `/notes/{id}/share` | Yes (owner only) |

---

## Example Usage

A complete flow from registration through sharing a note:

**Register**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "s3cur3P@ss"}'
```

**Login**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "s3cur3P@ss"}'
# Response: { "token": "<jwt>", "expiresIn": 86400000 }
```

**Create a note**
```bash
curl -X POST http://localhost:8080/notes \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title": "Shopping list", "content": "Milk, eggs, bread"}'
```

**List your notes**
```bash
curl http://localhost:8080/notes \
  -H "Authorization: Bearer <token>"
```

**Get a note by ID**
```bash
curl http://localhost:8080/notes/<note-id> \
  -H "Authorization: Bearer <token>"
```

**Update a note**
```bash
curl -X PUT http://localhost:8080/notes/<note-id> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"content": "Milk, eggs, bread, butter"}'
```

**Share a note**
```bash
curl -X POST http://localhost:8080/notes/<note-id>/share \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"username": "bob"}'
```

**Delete a note**
```bash
curl -X DELETE http://localhost:8080/notes/<note-id> \
  -H "Authorization: Bearer <token>"
```

---

## Security Considerations

- **Passwords** are hashed with BCrypt before storage; plaintext passwords are never persisted or logged.
- **JWT tokens** are signed with HMAC-SHA256 and expire after 24 hours (configurable via `app.jwt.expiration-ms`).
- **Authorization** is enforced at the service layer: update, delete, and share operations require the caller to be the note owner; read access is granted to the owner or any user the note has been explicitly shared with.
- **Out of scope for this project:** HTTPS termination (handled by a reverse proxy/load balancer in production), rate limiting, refresh tokens, account lockout.

---

## Assumptions & Tradeoffs

- **`ddl-auto: update`** is used for local development. A production deployment would replace this with Flyway or Liquibase migrations.
- **Hard deletes** — deleting a note permanently removes it and all associated share records (cascade). Soft deletes were not implemented as the spec did not require them.
- **No email or account verification** — usernames are the only identity. An email field and verification flow would be added for production.
- **Sharing is idempotent** — sharing a note with a user who already has access returns 200 without creating a duplicate record.
- **UUID primary keys** — avoids leaking sequential IDs and simplifies any future distributed deployment.

---

## Future Improvements

- Flyway migrations to replace `ddl-auto: update`
- Refresh token support with token rotation
- Pagination on `GET /notes`
- Full-text search on note content
- Audit log (who accessed or modified a note and when)
- Rate limiting on auth endpoints

---

## Design Considerations

<details>
<summary>Production deployment</summary>

Containerise the application with a `Dockerfile` and deploy via a container orchestrator (e.g. Kubernetes or AWS ECS). The PostgreSQL instance would move to a managed service (RDS, Cloud SQL). Environment-specific config (secrets, DB URL) would be injected via environment variables or a secrets manager. A reverse proxy (nginx, ALB) handles TLS termination in front of the app.

</details>

<details>
<summary>Observability</summary>

Expose Spring Boot Actuator health and metrics endpoints and scrape them with Prometheus/Grafana. Key signals to alert on: HTTP 5xx error rate, p99 request latency, DB connection pool saturation, and JWT validation failure rate (a spike suggests a scanning attempt). Structured JSON logging with a correlation ID per request aids debugging.

</details>

<details>
<summary>Database migrations</summary>

Replace `ddl-auto: update` with Flyway. Migration scripts live in `src/main/resources/db/migration` and run automatically on startup. Each migration is versioned (`V1__create_users.sql`, `V2__create_notes.sql`, etc.). Rollback scripts are maintained alongside forward migrations for any destructive change.

</details>

<details>
<summary>Scaling to 10,000 concurrent users</summary>

The stateless JWT design means any number of app instances can run behind a load balancer without session affinity. The primary bottleneck at that scale is the database: a read replica handles `GET` traffic while the primary handles writes. Connection pooling (HikariCP, already included via Spring Boot) is tuned to match the DB's `max_connections`. If sharing queries become expensive, a denormalised access-control table or a Redis-backed cache of `(user_id, note_id)` access grants reduces hot-path DB load.

</details>

---

> **Implementation status:** Base project scaffolding is complete (Spring Boot, Gradle, Docker Compose, package structure). Feature implementation (auth, notes CRUD, sharing) is in progress.
