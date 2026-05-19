Create or update `README.md` in the project root to satisfy the documentation requirements in `Docs/REQUIREMENTS.md`.

## Instructions

### 1. Gather current project state
Read the following before writing anything:
- `Docs/REQUIREMENTS.md` — the documentation checklist this README must satisfy
- `Docs/DOMAIN.md` — architecture, entities, package structure, design decisions
- `Docs/features/auth/SPEC.md`, `Docs/features/auth/PROGRESS.md`
- `Docs/features/notes-crud/SPEC.md`, `Docs/features/notes-crud/PROGRESS.md`
- `Docs/features/note-sharing/SPEC.md`, `Docs/features/note-sharing/PROGRESS.md`
- `build.gradle.kts` — to confirm actual dependency and Java version choices
- `docker-compose.yml` — to document the exact run command
- `src/main/resources/application.yml` — for config reference
- Run `git log --oneline` to understand what has been implemented so far

### 2. Assess implementation status
For each feature (auth, notes CRUD, sharing), determine whether it is:
- Fully implemented (code exists and tests pass)
- Partially implemented (some code exists, stubs or gaps remain)
- Not yet started

Use this to accurately represent the current state in the README rather than writing as if everything is complete.

### 3. Write the README

Produce a `README.md` that covers every item required by `Docs/REQUIREMENTS.md`:

**a) System overview and architecture**
- What the service does in 2–3 sentences
- High-level architecture (REST API → Service layer → JPA → PostgreSQL)
- Package structure from `Docs/DOMAIN.md`

**b) Tech stack choices and rationale**
- Language/framework (Java 17, Spring Boot, why)
- Database (PostgreSQL via Docker, why)
- Auth approach (JWT via jjwt, why)
- Build tool (Gradle Kotlin DSL, why)

**c) How to run the project and tests**
- Prerequisites (Java 17, Docker)
- Start the database: `docker compose up -d`
- Run the application: `./gradlew bootRun`
- Run tests: `./gradlew test`
- Any environment variables (e.g. `JWT_SECRET`)

**d) API usage examples**
- One `curl` example per endpoint from the requirements table
- Show a realistic flow: register → login → create note → share note → access as recipient

**e) Security considerations**
- Password hashing (BCrypt)
- JWT signing and expiry
- Authorization checks (ownership vs. shared access)
- What is explicitly out of scope (rate limiting, HTTPS termination, etc.)

**f) Assumptions, tradeoffs, and future improvements**
- Key assumptions made (e.g., username uniqueness, no email verification)
- Tradeoffs chosen (e.g., `ddl-auto: update` vs. migrations, hard deletes)
- What would be improved given more time

**g) Optional: Design considerations** (add a collapsible section or brief paragraph)
Address the four topics from `Docs/REQUIREMENTS.md` optional enhancements:
- Production deployment
- Observability / monitoring
- Database migrations over time
- Scaling to 10,000 concurrent users

### 4. Accuracy rules
- Do not document endpoints or features as working if they are not yet implemented — instead note them as "planned" or "in progress".
- Do not invent configuration values; use what is in `application.yml`.
- The run commands must exactly match what works given the current project setup.

### 5. Write the file
Write the completed content to `README.md` in the project root. If a `README.md` already exists, update it in place rather than replacing sections the user may have customized — preserve any content not covered by the requirements checklist.

After writing, report which required sections were populated from real implementation evidence vs. which were written as stubs because the feature isn't built yet.
