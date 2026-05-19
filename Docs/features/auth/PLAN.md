# Plan: Authentication

## Approach
Layered implementation bottom-up: migration → entity → repository → security primitives → service → controller → tests. No speculative abstractions; each class maps 1:1 to a responsibility in the spec.

## Task Breakdown
- [x] Migration: `V1__create_users_table.sql` (see [MIGRATIONS.md](../../MIGRATIONS.md))
- [x] `User` JPA entity
- [x] `UserRepository` with `findByUsername`
- [x] `UserDetailsServiceImpl`
- [x] `JwtTokenService` (generate, validate, parse)
- [x] `JwtAuthFilter` (`OncePerRequestFilter`)
- [x] `SecurityConfig` (permit `/auth/**`, require auth elsewhere)
- [x] `AuthService` (register + login)
- [x] `AuthController` (POST /auth/register, POST /auth/login)
- [x] DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`
- [x] Global error response envelope (`@ControllerAdvice`)
- [x] Tests (see SPEC.md test scenarios)

## Tasks Added During Implementation
- [x] `ErrorResponse` DTO (error envelope record — not listed in original plan but required by spec)
- [x] `UsernameAlreadyTakenException` (custom exception needed for clean 409 handling)
- [x] Add `spring-boot-flyway` dependency to `build.gradle.kts` (Spring Boot 4 requires explicit module)

## Dependencies
- None — this feature is the foundation all others depend on.

## Open Questions
_All resolved. See SPEC.md § As Built for decisions made._
