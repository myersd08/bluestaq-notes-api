# Feature: Authentication

## Summary
Users register with a username and password. On login they receive a JWT that must be included as a `Bearer` token on all subsequent requests.

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
