# Plan: Authentication

## Approach
_To be filled before implementation begins._

## Task Breakdown
- [ ] Migration: `V1__create_users_table.sql` (see [MIGRATIONS.md](../../MIGRATIONS.md))
- [ ] `User` JPA entity
- [ ] `UserRepository` with `findByUsername`
- [ ] `UserDetailsServiceImpl`
- [ ] `JwtTokenService` (generate, validate, parse)
- [ ] `JwtAuthFilter` (`OncePerRequestFilter`)
- [ ] `SecurityConfig` (permit `/auth/**`, require auth elsewhere)
- [ ] `AuthService` (register + login)
- [ ] `AuthController` (POST /auth/register, POST /auth/login)
- [ ] DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`
- [ ] Global error response envelope (`@ControllerAdvice`)
- [ ] Tests (see SPEC.md test scenarios)

## Dependencies
- None — this feature is the foundation all others depend on.

## Open Questions
_Questions to resolve before or during implementation._
