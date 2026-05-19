# Plan: Notes CRUD

## Approach
_To be filled before implementation begins._

## Task Breakdown
- [ ] Migration: `V2__create_notes_table.sql` (see [MIGRATIONS.md](../../MIGRATIONS.md))
- [ ] `Note` JPA entity
- [ ] `NoteRepository` with `findByOwnerId`
- [ ] `NoteService` (create, list, get, update, delete + auth checks)
- [ ] `NoteController` (all `/notes` routes)
- [ ] DTOs: `CreateNoteRequest`, `UpdateNoteRequest`, `NoteResponse`
- [ ] Tests (see SPEC.md test scenarios)

## Dependencies
- Auth feature must be complete — note ownership is derived from the JWT subject.

## Open Questions
_Questions to resolve before or during implementation._
