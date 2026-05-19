# Progress: Notes CRUD

## Status
Fully implemented — all 10 SPEC test scenarios pass.

---

## Sessions

### 2026-05-19
**Goal:** Implement the full Notes CRUD feature per SPEC.md and PLAN.md.

**Completed:**
- V2 Flyway migration (`notes` table)
- V3 Flyway migration (`shares` table with `ON DELETE CASCADE`) — pulled forward from note-sharing feature
- `Note` JPA entity with `@PrePersist` / `@PreUpdate` timestamp callbacks
- `Share` JPA entity (minimal — stores `note_id` + `shared_with_user_id` as UUIDs)
- `NoteRepository` (`findByOwnerId`) and `ShareRepository` (`existsByNoteIdAndSharedWithUserId`)
- `NoteService` — full CRUD + ownership/share authorization checks
- `NoteController` — `POST/GET/PUT/DELETE /notes` using `@AuthenticationPrincipal`
- DTOs: `CreateNoteRequest`, `UpdateNoteRequest`, `NoteResponse` (Java records)
- Exception classes: `NoteNotFoundException` (404), `NoteAccessDeniedException` (403), `NoUpdateFieldsException` (400)
- `GlobalExceptionHandler` extended with three new `@ExceptionHandler` methods
- `NoteControllerTest` — all 10 SPEC scenarios covered and passing
- `AuthControllerTest.setUp()` fixed to delete shares → notes → users in FK-safe order

**Decisions made:**
- V3 migration included here (not deferred) because the shared-access test scenario requires it.
- Used `JsonPath.read()` instead of `@Autowired ObjectMapper` in tests — `ObjectMapper` is not exposed as a bean in the `@SpringBootTest` + `@AutoConfigureMockMvc` context under Spring Boot 4.
- No `@ManyToOne` JPA associations — UUIDs stored as plain columns, consistent with the codebase.
- Cascade delete of shares is handled at the DB level (`ON DELETE CASCADE`), not in application code.

**Blockers:** None.

**Next:** Note-sharing feature (V3 migration is already applied; `Share` entity and `ShareRepository` exist).
