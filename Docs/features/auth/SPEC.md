# Feature: Authentication

## Summary
Users register with a username and password. On login they receive a JWT that must be included as a `Bearer` token on all subsequent requests.

---

## As Built

**Status:** Fully implemented

**Planned vs. built:** Built as specified. All endpoints, response shapes, HTTP status codes, and test scenarios match the spec exactly.

**Design & architecture decisions:**

- **`User` implements `UserDetails` directly.** Rather than a separate wrapper class, the JPA entity itself satisfies Spring Security's interface. This eliminates an extra class; the trade-off is a tighter coupling between the persistence model and the security framework. Acceptable here because the project has no second auth mechanism that would need to diverge.
- **`JwtAuthFilter` loads user by UUID from `UserRepository`, not via `UserDetailsService`.** The JWT `sub` claim stores the user UUID (per spec), so looking up by username would require a round-trip to parse the UUID back to a username. The filter injects `UserRepository` directly and calls `findById(UUID)` instead.
- **JWT signing key derived from raw UTF-8 bytes.** `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` rather than base64-decode, because the default secret in `application.yml` is a plain string. The string is long enough (71 chars = 568 bits) for HMAC-SHA256.
- **Custom `AuthenticationEntryPoint` added to `SecurityConfig`.** Spring Security 7 (shipped with Spring Boot 4) returns 403 by default for anonymous requests to protected endpoints. An explicit entry point calling `res.sendError(401)` was required to satisfy the spec's "no token → 401" requirement.
- **`spring-boot-flyway` module added to `build.gradle.kts`.** Spring Boot 4 split Flyway autoconfiguration into a standalone module (`org.springframework.boot:spring-boot-flyway`) that is not pulled in transitively. Without it, Flyway silently never ran.
- **Docker port changed from 5432 → 5434.** The dev machine has a native PostgreSQL instance on 5432; the Docker container was remapped to 5434 in both `docker-compose.yml` and `application.yml` to avoid the conflict.

**Gotchas & constraints:**

- **Spring Boot 4 `@AutoConfigureMockMvc` moved.** The annotation is now at `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, not the Spring Boot 3 path `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc`.
- **Tests require a live PostgreSQL on port 5434.** There is no H2 fallback. Run `docker compose up -d` before `./gradlew test`.
- **`./gradlew test` leaves a JVM process on port 8081.** If `bootRun` follows immediately and fails with "port in use", kill the lingering process: `Get-NetTCPConnection -LocalPort 8081 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }`.
- **`LoginRequest` has no `@NotBlank` validation.** Missing/null username falls through to a `BadCredentialsException` (→ 401) rather than a 400. This is intentional — the spec only requires 400 for registration, and revealing which field is wrong on login is a security leak.

---

## Endpoints

### POST /auth/register
Register a new user account.

**Request**
```json
{
  "username": "alice",
  "password": "s3cur3P@ss"
}
```

**Responses**

| Status | Condition |
|--------|-----------|
| 201    | User created successfully |
| 400    | Missing/invalid fields (validation failure) |
| 409    | Username already taken |

**Response body (201)**
```json
{
  "id": "uuid",
  "username": "alice",
  "createdAt": "2026-05-19T12:00:00Z"
}
```

---

### POST /auth/login
Authenticate and receive a JWT.

**Request**
```json
{
  "username": "alice",
  "password": "s3cur3P@ss"
}
```

**Responses**

| Status | Condition |
|--------|-----------|
| 200    | Credentials valid |
| 401    | Invalid username or password |

**Response body (200)**
```json
{
  "token": "<jwt>",
  "expiresIn": 86400000
}
```

---

## Business Logic

1. On register: hash password with BCrypt, persist `User`, return 201.
2. On login: load user by username, verify BCrypt hash, generate and return JWT.
3. JWT claims: `sub` = user ID (UUID string), `iat`, `exp`.
4. Subsequent requests: `JwtAuthFilter` extracts and validates the token, populates `SecurityContext`.

---

## Error Responses
All errors follow a consistent envelope:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "username must not be blank"
}
```

---

## Classes to Implement

| Layer      | Class | Responsibility |
|------------|-------|----------------|
| Controller | `AuthController` | Handle register/login HTTP |
| Service    | `AuthService` | Register + login logic |
| Security   | `JwtTokenService` | Generate, validate, parse JWT |
| Security   | `JwtAuthFilter` | `OncePerRequestFilter` for token extraction |
| Security   | `UserDetailsServiceImpl` | Load `User` for Spring Security |
| Config     | `SecurityConfig` | Permit `/auth/**`, require auth elsewhere |
| DTO        | `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse` | |
| Model      | `User` | JPA entity |
| Repository | `UserRepository` | `findByUsername` |

---

## Test Scenarios

- Register with valid credentials → 201, user persisted
- Register with duplicate username → 409
- Register with blank username → 400
- Login with correct credentials → 200 + JWT
- Login with wrong password → 401
- Call protected endpoint without token → 401
- Call protected endpoint with expired token → 401
