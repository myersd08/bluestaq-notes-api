Distill the results of a completed or in-progress feature implementation back into the project's domain documentation.

**Feature to distill:** $ARGUMENTS

## Instructions

Follow these steps in order:

### 1. Identify the feature folder
If `$ARGUMENTS` is provided, match it to the corresponding folder under `Docs/features/` (e.g. `auth` → `Docs/features/auth/`, `notes` → `Docs/features/notes-crud/`, `sharing` → `Docs/features/note-sharing/`). If not provided, ask the user which feature to distill.

### 2. Read the current state of docs
- Read `Docs/DOMAIN.md`
- Read `Docs/features/<feature>/SPEC.md`
- Read `Docs/features/<feature>/PLAN.md`
- Read `Docs/features/<feature>/PROGRESS.md`

### 3. Gather implementation evidence
- Run `git diff main...HEAD --name-only` to list changed files for this feature.
- Run `git log main...HEAD --oneline` to see commit messages.
- For each key source file changed (models, services, controllers, config, security), read it and note: actual class names, method signatures, any schema or design that differs from the feature doc.

### 4. Produce a distillation

Write a distillation summary and update `Docs/DOMAIN.md` by appending (or updating) an `## Implemented Features` section. For the feature being distilled, record:

**a) Status** — Fully implemented / Partially implemented / Stubbed

**b) What was built** — 2–4 bullet points covering what actually exists in code (not what was planned).

**c) Key files** — Table of the most important files with a one-line description of their role.

**d) Deviations from the feature doc** — Any design decisions made during implementation that differ from what the feature doc specified. If none, say so explicitly.

**e) Questions & answers** — Any ambiguities that came up and how they were resolved. If none arose, note that too.

**f) Remaining work** — Anything listed in the feature doc's test scenarios or class list that was not yet implemented.

### 5. Update the feature folder docs

**SPEC.md — As Built section**
Insert (or overwrite) an `## As Built` section immediately after the `## Summary` section. This is the canonical "what actually shipped" snapshot and should be kept current every time distill is run. Include:

- **Status** — Fully implemented / Partially implemented / Stubbed
- **Planned vs. built** — A concise comparison: what the spec called for and what actually exists. Use a two-column table if there are notable differences; a single sentence ("built as specified") if there are none.
- **Design & architecture decisions** — Decisions made during implementation that shaped the code: why a class was split, a pattern chosen, a shortcut taken, or a spec detail changed. One bullet per decision, written so a future reader understands both the choice and the reason.
- **Gotchas & constraints** — Non-obvious things a future developer needs to know: framework quirks, ordering dependencies, security constraints, etc.

**PLAN.md**
Check off completed tasks. Note any tasks that were added or removed during implementation.

**PROGRESS.md**
Append a new session block with today's date, what was completed, decisions made, and any remaining blockers.

### 6. Report back
After updating both docs, give the user a one-paragraph summary of what changed and flag anything that looks inconsistent or needs a decision.
