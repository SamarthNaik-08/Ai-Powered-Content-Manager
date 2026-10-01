# Phase 1 Analysis: Foundation + Authentication + RBAC

## 1. Current Repository State
- **Workspace Path:** `c:\Projects\ai-powered-content-manager`
- **Initial State:** Empty directory. No prior backend, frontend, or database migrations existed.
- **Environment Assessment:**
  - Java: OpenJDK 17.0.20.1 (Target: Java 17)
  - Maven: 3.9.6
  - Node.js: v24.11.1
  - npm: 11.14.1
  - Docker: 29.7.2

## 2. Architectural Blueprint for Phase 1
To ensure high cohesion, low coupling, and production standards, the project will be organized as follows:

```
ai-powered-content-manager/
├── backend/                  # Spring Boot 3.x Java 17 Modular Monolith
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/contentmanager/
│       │   │   ├── common/             # ApiResponse, GlobalExceptionHandler, BaseEntity, Constants
│       │   │   ├── config/             # SecurityConfig, CorsConfig, OpenApiConfig, JpaAuditingConfig
│       │   │   ├── security/           # JwtTokenProvider, JwtAuthFilter, UserPrincipal, UserDetailsServiceImpl
│       │   │   └── modules/
│       │   │       ├── auth/           # AuthController, AuthService, DTOs (Login, Register, Refresh)
│       │   │       ├── user/           # User, Role, Permission entities, Repositories, UserService, UserDto, UserMapper
│       │   │       └── rbac/           # RoleName, PermissionName enums
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/       # V1__initial_schema.xml / .sql (Flyway migrations)
│       └── test/                       # Unit & Slice/Integration Tests
├── frontend/                 # React 18 / 19 + TypeScript + Vite + Ant Design 5
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── app/              # Router, QueryClient, App Providers
│       ├── components/       # Common UI: AppHeader, Sidebar, ProtectedRoute, Shell
│       ├── features/
│       │   ├── auth/         # Login, Register, ForgotPassword, ResetPassword, useAuth
│       │   ├── dashboard/    # Overview Dashboard
│       │   └── placeholders/ # Placeholders for Content, AI, Media, etc.
│       ├── layouts/          # DashboardLayout, AuthLayout
│       ├── services/         # Axios instance, Auth API, Token management
│       ├── types/            # Auth, User, API Response interfaces
│       └── utils/            # Token storage, helpers
├── docker-compose.yml        # PostgreSQL 16 + Redis (development infrastructure)
└── docs/                     # Documentation files
```

## 3. Database Schema Design (Flyway `V1__init_auth_rbac.sql`)
1. **`permissions`**: `id (BIGSERIAL PK)`, `name (VARCHAR UNIQUE)`, `description`, `created_at`, `updated_at`
2. **`roles`**: `id (BIGSERIAL PK)`, `name (VARCHAR UNIQUE)` (ADMIN, EDITOR, AUTHOR, VIEWER), `description`, `created_at`, `updated_at`
3. **`role_permissions`**: `role_id (FK)`, `permission_id (FK)`
4. **`users`**: `id (BIGSERIAL PK)`, `uuid (UUID UNIQUE)`, `name`, `email (VARCHAR UNIQUE)`, `password_hash`, `status` (ACTIVE, INACTIVE, SUSPENDED), `created_at`, `updated_at`, `last_login_at`
5. **`user_roles`**: `user_id (FK)`, `role_id (FK)`
6. **`refresh_tokens`**: `id (BIGSERIAL PK)`, `token (VARCHAR UNIQUE)`, `user_id (FK)`, `revoked (BOOLEAN)`, `expires_at (TIMESTAMP)`, `created_at`
- Indexes placed on `email`, `uuid`, `token`, `expires_at`, and foreign keys.

## 4. Security & RBAC Design
- **Password Hashing:** `BCryptPasswordEncoder` (strength 12).
- **Authentication:** Stateless JWT using standard claims (`sub` = UUID or email, custom claim `roles`, `permissions`, `uid`).
- **Token Pair:**
  - Access Token: short-lived (15 minutes by default).
  - Refresh Token: persisted in DB with revocation flag, expiry check, single-use rotation or revocation on logout.
- **Method Security:** `@PreAuthorize("hasAuthority('CONTENT_READ')")` or `@PreAuthorize("hasRole('ADMIN')")`.
- **Response Format:** Uniform `ApiResponse<T>` with `{ success, message, data, timestamp }` and `ApiResponse<Void>` for errors `{ success: false, message, errors, timestamp }`.

## 5. Frontend Design
- Single-page application using React, TypeScript, Vite.
- Ant Design 5 with curated modern styling and dark/light system coherence.
- React Router 6/7 with Route Guards (`ProtectedRoute`).
- TanStack Query v5 + Axios with interceptors for attaching Bearer token and automatic refresh token renewal.
- Form validation with React Hook Form + Zod.
