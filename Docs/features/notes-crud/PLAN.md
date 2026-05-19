# Plan: Notes CRUD

## Approach
Implement full CRUD for notes with JWT-based ownership enforcement. The `GET /notes/{id}` endpoint also requires checking the `shares` table, so V3 migration and the `Share` entity are included in this feature to satisfy the "Get note shared with caller" test scenario.

## Task Breakdown
- [x] Migration: `V2__create_notes_table.sql` (see [MIGRATIONS.md](../../MIGRATIONS.md))
- [x] Migration: `V3__create_shares_table.sql` — required by `GET /notes/{id}` shared-access check
- [x] `Note` JPA entity
- [x] `Share` JPA entity (minimal — owner/read-access check only)
- [x] `NoteRepository` with `findByOwnerId`
- [x] `ShareRepository` with `existsByNoteIdAndSharedWithUserId`
- [x] `NoteService` (create, list, get, update, delete + auth checks)
- [x] `NoteController` (all `/notes` routes)
- [x] DTOs: `CreateNoteRequest`, `UpdateNoteRequest`, `NoteResponse`
- [x] Exceptions: `NoteNotFoundException`, `NoteAccessDeniedException`, `NoUpdateFieldsException`
- [x] `GlobalExceptionHandler` updated with 404 / 403 / 400 handlers
- [x] `NoteControllerTest` — all 10 SPEC scenarios pass
- [x] `AuthControllerTest` updated — `setUp()` now deletes shares → notes → users to respect FK order

## Dependencies
- Auth feature must be complete — note ownership is derived from the JWT subject.

## Open Questions
_None — all resolved during implementation._
