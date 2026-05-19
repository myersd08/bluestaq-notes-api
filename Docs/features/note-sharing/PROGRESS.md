# Progress: Note Sharing

## Status
Fully implemented — all 8 test scenarios pass.

---

## Sessions

### 2026-05-19
**Goal:** Implement Note Sharing feature end-to-end.

**Completed:**
- Added `findByNoteIdAndSharedWithUserId` to `ShareRepository` (idempotency lookup)
- Created `SelfShareException` (400) and `UserNotFoundException` (404)
- Created `ShareRequest` and `ShareResponse` DTOs (Java records)
- Created `ShareService` with full business logic (steps 1–5 from SPEC): note lookup, ownership check, user lookup, self-share guard, idempotency check, persist
- Added `POST /notes/{id}/share` route to `NoteController`; wired `ShareService` dependency
- Extended `GlobalExceptionHandler` with handlers for `UserNotFoundException` (404) and `SelfShareException` (400)
- Wrote `NoteShareControllerTest` — 8 self-contained integration tests covering all SPEC scenarios
- Updated `PLAN.md` to reflect pre-existing infrastructure from notes-crud feature
- All 26 tests across 4 suites pass (8 new + 10 notes-crud + 7 auth + 1 context load)

**Decisions made:**
- Self-share returns **400** (not 422 per SPEC) — reclassified as request validation failure; consistent with existing 400 handling for `@NotBlank` on the same endpoint.
- Idempotency uses `findByNoteIdAndSharedWithUserId` (returns the existing record) rather than `existsByNoteIdAndSharedWithUserId` + re-query — avoids a second DB round-trip and preserves original `createdAt` in the response.
- Ownership check runs before user lookup (step 2 before step 3) — non-owners get 403 even if the target username is invalid, matching the SPEC's business logic ordering.
- `ShareService` kept separate from `NoteService` — share-creation concerns are distinct from CRUD; both are wired into `NoteController`.

**Blockers:** None.

**Next:** None — feature complete. Ready to commit.

### 2026-05-19 (correction)
**Goal:** Correct self-share status code from 400 → 422 to match SPEC and HTTP semantics.

**Completed:**
- `GlobalExceptionHandler`: `SelfShareException` handler changed from `HttpStatus.BAD_REQUEST` / 400 to `HttpStatus.UNPROCESSABLE_ENTITY` / 422
- `NoteShareControllerTest` scenario 5: assertion updated from `isBadRequest()` / 400 to `isUnprocessableEntity()` / 422
- All 26 tests pass

**Decisions made:**
- 422 is correct: the request is well-formed and the username resolves to a real user — the rejection is on semantic/business grounds (caller equals target), not malformation. The REQUIREMENTS only say "use appropriate HTTP status codes"; 422 is the appropriate one here.

**Blockers:** None.

**Next:** None — feature complete. Ready to commit.

<!--
Copy this block for each work session:

### YYYY-MM-DD
**Goal:**
**Completed:**
**Decisions made:**
**Blockers:**
**Next:**
-->
