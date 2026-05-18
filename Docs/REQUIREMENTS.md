# Secure Notes Vault API — Requirements

## Overview

Build a small backend service demonstrating API design, data persistence, testing, and documentation. Users can create, view, manage, and share notes with authentication and access control.

- Any language, framework, or tooling is acceptable
- Must run locally
- Focus: clarity, correctness, and tradeoffs — not completeness or over-engineering

**Time expectation:** ~3–6 hours. Stubs and documented reasoning for unimplemented components are acceptable.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Authenticate and receive a token |
| POST | `/notes` | Create a new note |
| GET | `/notes` | List the authenticated user's notes |
| GET | `/notes/{id}` | Get a note by ID (if authorized) |
| PUT | `/notes/{id}` | Update a note (if authorized) |
| DELETE | `/notes/{id}` | Delete a note (if authorized) |
| POST | `/notes/{id}/share` | Share a note with another user (read-only) |

---

## Functional Requirements

### API Behavior
- Use appropriate HTTP status codes
- Return clear error responses for invalid requests
- Behavior must be predictable and documented

### Authentication & Authorization
- Token-based authentication (e.g., JWT)
- Users can only access their own notes unless a note has been explicitly shared with them
- Shared notes are **read-only** to the recipient

---

## Data Model

Minimum required schema:

```
User:  id, username, password_hash, created_at
Note:  id, owner_id, content, created_at, updated_at
Share: id, note_id, shared_with_user_id, created_at
```

Schema may be adjusted or extended — be prepared to explain choices.

---

## Data Storage

- Use a relational or embedded database (e.g., PostgreSQL, SQLite, H2)
- Document the choice and any schema decisions

---

## Testing

Required automated tests:
- API-level tests covering at least authentication and access control scenarios
- Business logic or data-layer tests
- At least one test verifying a user **cannot** access another user's note

---

## Documentation (README.md)

Must include:
- System overview and architecture
- Tech stack choices and rationale
- How to run the project and tests
- API usage examples
- Security considerations
- Assumptions, tradeoffs, and future improvements

---

## Build & Submission

The project must run locally via one of:
- A single command
- Docker / Docker Compose
- A clearly documented setup with no external dependencies beyond the database

Submit via a version-controlled repository (GitHub, GitLab, etc.) with all source code, tests, and documentation.

---

## Optional Enhancements

Address briefly in `README.md` or a separate `DESIGN.md`:

1. **Production deployment** — How would you deploy this service?
2. **Observability** — What would you monitor or alert on?
3. **Database migrations** — How would you handle schema changes over time?
4. **Scalability** — What would change to support 10,000 concurrent users?

A few sentences each is sufficient — the goal is demonstrating production-minded thinking.
