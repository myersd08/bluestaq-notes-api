Review the current working tree and create a git commit with a well-crafted message. Optionally scope it with: $ARGUMENTS

## Instructions

### 1. Understand what changed
Run these in parallel:
- `git status` — see staged and unstaged files
- `git diff HEAD` — full diff of all changes (staged + unstaged)
- `git log --oneline -5` — check existing commit style to match it

### 2. Analyze the changes
Group the changed files by concern (e.g. "feature implementation", "docs", "config", "tests"). Cross-reference with the feature docs in `Docs/features/` if the changes relate to a known feature. If `$ARGUMENTS` was provided, use it to scope what gets committed (e.g. `/commit auth` would only stage files related to auth).

### 3. Draft the commit message
Follow this format:

```
<type>(<scope>): <short imperative summary under 72 chars>

- Bullet describing the first notable change
- Bullet describing the second notable change
- (add more only if genuinely distinct — omit if redundant with the subject line)
```

**Type** — pick one:
- `feat` — new functionality
- `docs` — documentation only
- `config` — build files, app config, docker
- `test` — tests only
- `refactor` — code change with no behaviour change
- `fix` — bug fix
- `chore` — tooling, scaffolding, cleanup

**Scope** — the affected area in one word (e.g. `auth`, `notes`, `sharing`, `readme`, `domain`, `setup`). Omit if the change spans multiple unrelated areas.

**Rules:**
- Subject line is imperative mood ("add auth filter", not "added" or "adds")
- No period at the end of the subject line
- Body bullets only for changes that aren't obvious from the subject
- Do not mention file names in the subject line unless the file IS the feature (e.g. `README.md`)
- Do not reference issue numbers or task IDs unless the user provides one

### 4. Show the plan and confirm
Before committing, display:
1. The list of files that will be staged
2. The full draft commit message

Then ask the user: **"Commit with this message? (yes / edit / cancel)"**

- **yes** — proceed
- **edit** — ask the user what to change, update the message, confirm again
- **cancel** — stop without committing

### 5. Commit
If confirmed:
1. `git add` the relevant files (prefer specific paths over `git add .`)
2. `git commit -m "..."` using a heredoc to preserve formatting
3. Run `git log --oneline -1` to confirm the commit landed
4. Report the commit hash and subject line back to the user
