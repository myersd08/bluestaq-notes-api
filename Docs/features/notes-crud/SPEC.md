# Feature: Notes CRUD

## Summary
Authenticated users can create, list, retrieve, update, and delete their own notes. All endpoints require a valid JWT.

---

## As Built

**Status:** Fully implemented (2026-05-19)

**Planned vs. built:**

| Spec called for | What was built |
|-----------------|----------------|
| All 5 CRUD endpoints at `/notes` | Built as specified |
| `NoteController`, `NoteService`, `Note`, `NoteRepository`, DTOs | Built as specified |
| `GET /notes/{id}` returns note if owner OR shared | Built — requires `Share` entity and V3 migration (pulled forward from note-sharing feature) |
| `DELETE /notes/{id}` cascades to shares | Built — handled via `ON DELETE CASCADE` in V3 migration, not application code |
| `NoteRepository.findByIdAndOwnerIdOrShared` | Not implemented — `NoteService.get()` calls `findById()` then checks owner/share separately; simpler and equivalent |

**Design & architecture decisions:**
- V3 migration (`shares` table) and `Share` entity were created in this feature rather than deferred to note-sharing, because the "Get note shared with caller → 200" test scenario in this SPEC requires them to exist.
- `@AuthenticationPrincipal User caller` is used in every controller method to extract the authenticated user — the `User` entity is stored directly as the principal in `SecurityContext` by `JwtAuthFilter`.
- `updated_at` is managed by a `@PreUpdate` JPA callback on `Note`, not set explicitly in the service.
- All entity relationships (owner_id, note_id, shared_with_user_id) are stored as raw UUID columns rather than `@ManyToOne` FK references, consistent with the existing `User` entity pattern.
- Partial update semantics: `UpdateNoteRequest` has both fields nullable; the service applies only non-null fields and throws `NoUpdateFieldsException` (→ 400) if both are null.

**Gotchas & constraints:**
- `AuthControllerTest.setUp()` must delete shares → notes → users in that order. `userRepository.deleteAll()` alone violates the FK constraint from `notes.owner_id → users.id`.
- `ObjectMapper` is not autowirable in the `@SpringBootTest` + `@AutoConfigureMockMvc` context in Spring Boot 4 — use `com.jayway.jsonpath.JsonPath` for JSON value extraction in tests instead.
- The `shares` table unique constraint `uq_share (note_id, shared_with_user_id)` means attempting to share the same note with the same user twice will throw a DB constraint error; the note-sharing feature will need to handle that as idempotent (see DOMAIN.md business rule 5).

---

## Endpoints

### POST /notes — Create a note

**Request**
```json
{
  "title": "Shopping list",
  "content": "Milk, eggs, bread"
}
```

**Responses**

| Status | Condition |
|--------|-----------|
| 201    | Note created |
| 400    | Missing/invalid fields |
| 401    | Not authenticated |

**Response body (201)** — see [Note response shape](#note-response-shape).

---

### GET /notes — List the caller's notes

Returns only notes owned by the authenticated user. Does not include notes shared *with* the user (separate concern).

**Responses**

| Status | Condition |
|--------|-----------|
| 200    | Array of notes (may be empty) |
| 401    | Not authenticated |

**Response body (200)**
```json
[
  { "id": "uuid", "title": "...", "content": "...", "createdAt": "...", "updatedAt": "..." },
  ...
]
```

---

### GET /notes/{id} — Get a note by ID

Returns the note if the caller is the owner **or** the note has been shared with them.

**Responses**

| Status | Condition |
|--------|-----------|
| 200    | Note returned |
| 401    | Not authenticated |
| 403    | Authenticated but not authorized |
| 404    | Note does not exist |

---

### PUT /notes/{id} — Update a note

Owner only. Updates `title`, `content`, or both. Sets `updated_at` to now.

**Request** (all fields optional, at least one required)
```json
{
  "title": "Updated title",
  "content": "Updated content"
}
```

**Responses**

| Status | Condition |
|--------|-----------|
| 200    | Updated note returned |
| 400    | No updatable fields provided |
| 401    | Not authenticated |
| 403    | Caller is not the owner |
| 404    | Note does not exist |

---

### DELETE /notes/{id} — Delete a note

Owner only. Also deletes all associated `Share` records (cascade).

**Responses**

| Status | Condition |
|--------|-----------|
| 204    | Deleted successfully |
| 401    | Not authenticated |
| 403    | Caller is not the owner |
| 404    | Note does not exist |

---

## Note Response Shape

```json
{
  "id": "uuid",
  "title": "Shopping list",
  "content": "Milk, eggs, bread",
  "ownerId": "uuid",
  "createdAt": "2026-05-19T12:00:00Z",
  "updatedAt": "2026-05-19T12:00:00Z"
}
```

---

## Business Logic

1. On create: set `owner_id` from the JWT subject, persist, return 201.
2. On list: `WHERE owner_id = :callerId`.
3. On get: load note, check caller is owner OR has a `Share` record → return note or 403.
4. On update: load note, check caller is owner → apply partial update, set `updated_at`.
5. On delete: load note, check caller is owner → delete (cascade removes shares).

---

## Classes to Implement

| Layer      | Class | Responsibility |
|------------|-------|----------------|
| Controller | `NoteController` | Handle all `/notes` HTTP routes |
| Service    | `NoteService` | CRUD logic + authorization checks |
| Model      | `Note` | JPA entity |
| Repository | `NoteRepository` | `findByOwnerId`, `findByIdAndOwnerIdOrShared` |
| DTO        | `CreateNoteRequest`, `UpdateNoteRequest`, `NoteResponse` | |

---

## Test Scenarios

- Create note → 201, note persisted with correct owner
- List notes → only caller's own notes returned
- Get own note → 200
- Get note shared with caller → 200
- Get another user's note (not shared) → 403
- Get non-existent note → 404
- Update own note → 200, `updated_at` changes
- Update note owned by another user → 403
- Delete own note → 204, shares also removed
- Delete note owned by another user → 403
