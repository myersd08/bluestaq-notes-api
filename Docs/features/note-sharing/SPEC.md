# Feature: Note Sharing

## Summary
A note owner can share their note with another registered user, granting that user read-only access. Sharing is idempotent — sharing an already-shared note with the same user is a no-op.

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
