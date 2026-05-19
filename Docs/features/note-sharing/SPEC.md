# Feature: Note Sharing

## Summary
A note owner can share their note with another registered user, granting that user read-only access. Sharing is idempotent — sharing an already-shared note with the same user is a no-op.

---

## As Built

**Status:** Fully implemented (2026-05-19)

**Planned vs. built:**

| Aspect | Spec | Built |
|--------|------|-------|
| Self-share response | 422 Unprocessable Entity | 422 Unprocessable Entity — built as specified |
| All other status codes | As specified | As specified |
| `ShareService` | Planned | `service/ShareService.java` — implements steps 1–5 exactly |
| DTOs | `ShareRequest`, `ShareResponse` | Both implemented as Java records |
| `Share` entity + migration | To be created | Already existed from notes-crud feature |
| `existsByNoteIdAndSharedWithUserId` | To be created | Already existed from notes-crud feature |
| `findByNoteIdAndSharedWithUserId` | To be created | Added to `ShareRepository` this feature |
| `UserNotFoundException` | Not mentioned | Added — required to 404 on unknown target username |

**Design & architecture decisions:**
- `ShareService` is a dedicated service class rather than adding share logic to `NoteService`. Keeps ownership-check concerns in `NoteService` and share-creation concerns in `ShareService`; `NoteController` now depends on both.
- Self-share detection compares `target.getId()` to `caller.getId()` (UUID equality) rather than comparing usernames. This is correct because the caller is the authenticated `User` entity resolved by UUID from the JWT.
- The idempotency check uses `findByNoteIdAndSharedWithUserId` (returns `Optional<Share>`) rather than `existsByNoteIdAndSharedWithUserId` so the existing record's `createdAt` is returned on a re-share, preserving the original timestamp in the response.
- `SelfShareException` maps to 422 via `GlobalExceptionHandler`. The request is well-formed and the username resolves to a real user — rejection is on semantic/business grounds (caller is the target), making 422 the correct HTTP status.

**Gotchas & constraints:**
- `POST /notes/{id}/share` returns 200 (not 201) for both new and idempotent shares — the endpoint is a command, not a resource creation, so 200 is correct per spec.
- Ownership check happens before user lookup (step 2 before step 3). This means a non-owner targeting a non-existent username gets 403, not 404 — ownership gates everything downstream.
- `@Transactional` on `ShareService.share()` is required: without it, the save and the subsequent read of `createdAt` could see stale data in some JPA flush modes.
- Test teardown order: `shareRepository.deleteAll()` → `noteRepository.deleteAll()` → `userRepository.deleteAll()` — FK constraints require this order.

---

## Endpoints

### POST /notes/{id}/share — Share a note

**Request**
```json
{
  "username": "bob"
}
```

**Responses**

| Status | Condition |
|--------|-----------|
| 200    | Share created (or already existed — idempotent) |
| 400    | Missing/invalid fields |
| 401    | Not authenticated |
| 403    | Caller is not the note owner |
| 404    | Note does not exist, or target user does not exist |
| 422    | Caller attempts to share with themselves |

---

## Response Body (200)

```json
{
  "noteId": "uuid",
  "sharedWithUsername": "bob",
  "createdAt": "2026-05-19T12:00:00Z"
}
```

---

## Business Logic

1. Load note by `{id}` — 404 if missing.
2. Verify caller is the owner — 403 if not.
3. Resolve target user by `username` — 404 if not found.
4. Reject if target user is the owner — 422.
5. Check for existing `Share(note_id, shared_with_user_id)`:
   - If it exists, return 200 with the existing record (idempotent).
   - If not, insert and return 200.

---

## Access Control Interaction

Once shared, the recipient can call `GET /notes/{id}` and receive the note content. They cannot call `PUT`, `DELETE`, or `POST /notes/{id}/share` — those remain owner-only.

See [notes-crud/SPEC.md](../notes-crud/SPEC.md) for the access check logic on `GET /notes/{id}`.

---

## Classes to Implement

| Layer      | Class | Responsibility |
|------------|-------|----------------|
| Controller | `NoteController` (existing) | Add `POST /notes/{id}/share` route |
| Service    | `ShareService` | Share creation logic and idempotency check |
| Model      | `Share` | JPA entity with unique constraint on `(note_id, shared_with_user_id)` |
| Repository | `ShareRepository` | `findByNoteIdAndSharedWithUserId`, `existsByNoteIdAndSharedWithUserId` |
| DTO        | `ShareRequest`, `ShareResponse` | |

---

## Test Scenarios

- Owner shares note with a valid user → 200, share persisted
- Owner shares again with same user → 200, no duplicate share created
- Non-owner attempts to share → 403
- Share with non-existent username → 404
- Owner shares note with themselves → 422
- Shared recipient calls `GET /notes/{id}` → 200
- Shared recipient calls `PUT /notes/{id}` → 403
- Shared recipient calls `DELETE /notes/{id}` → 403
