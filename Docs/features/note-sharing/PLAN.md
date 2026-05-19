# Plan: Note Sharing

## Approach
Dedicated `ShareService` wired into existing `NoteController`. Idempotency handled by `findByNoteIdAndSharedWithUserId` before insert. Self-share mapped to 400 (request validation failure) rather than 422. All infrastructure (`Share` entity, migration, `existsByNoteIdAndSharedWithUserId`) was inherited from notes-crud.

## Task Breakdown
- [x] Migration: `V3__create_shares_table.sql` — applied as part of notes-crud feature
- [x] `Share` JPA entity with unique constraint on `(note_id, shared_with_user_id)` — `model/Share.java` exists
- [x] `ShareRepository` (`existsByNoteIdAndSharedWithUserId`) — exists
- [x] `ShareRepository.findByNoteIdAndSharedWithUserId` — added for idempotency lookup
- [x] Update `NoteService.getById` to check share grants — already done in notes-crud (`NoteService.get()`)
- [x] `ShareService` (share logic + idempotency check) — `service/ShareService.java`
- [x] `POST /notes/{id}/share` route on `NoteController`
- [x] DTOs: `ShareRequest`, `ShareResponse` — both Java records in `dto/`
- [x] `SelfShareException` (400) and `UserNotFoundException` (404) — added, wired into `GlobalExceptionHandler`
- [x] Tests — `NoteShareControllerTest` (8 scenarios, all pass)

## Deviations
- `UserNotFoundException` added (not in original plan) — required to handle unknown target username as 404.
- Self-share initially implemented as 400, corrected to 422 to match SPEC — 422 is correct because the request is semantically invalid (valid username that is the caller), not malformed.

## Dependencies
- Auth feature must be complete.
- Notes CRUD feature must be complete — sharing operates on existing notes.

## Open Questions
_Questions to resolve before or during implementation._
