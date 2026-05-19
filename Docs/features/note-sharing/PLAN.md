# Plan: Note Sharing

## Approach
_To be filled before implementation begins._

## Task Breakdown
- [ ] Migration: `V3__create_shares_table.sql` (see [MIGRATIONS.md](../../MIGRATIONS.md))
- [ ] `Share` JPA entity with unique constraint on `(note_id, shared_with_user_id)`
- [ ] `ShareRepository` (`existsByNoteIdAndSharedWithUserId`, `findByNoteIdAndSharedWithUserId`)
- [ ] `ShareService` (share logic + idempotency check)
- [ ] `POST /notes/{id}/share` route on `NoteController`
- [ ] Update `NoteService.getById` to check share grants (if not already done in notes-crud)
- [ ] DTOs: `ShareRequest`, `ShareResponse`
- [ ] Tests (see SPEC.md test scenarios)

## Dependencies
- Auth feature must be complete.
- Notes CRUD feature must be complete — sharing operates on existing notes.

## Open Questions
_Questions to resolve before or during implementation._
