# AI-Powered Content Manager

A production-grade, secure, modern Content Management Platform built with **Spring Boot 3 (Java 17)**, **PostgreSQL 16**, **Redis 7**, **Flyway**, and **React 18 + TypeScript + Vite + Ant Design 5**.

---

## Architecture Overview

```
                                +---------------------------+
                                |  React 18 + TS + Vite     |
                                |  Ant Design 5 + Axios     |
                                +-------------+-------------+
                                              |
                                              | HTTPS / JSON (Port 80 via Nginx)
                                              v
+----------------------------------------------------------------------------------------+
| Spring Boot 3.3.x Modular Monolith (Port 8080)                                         |
|                                                                                        |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|  | Authentication API |   | Authorization / RBAC  |   | Security & JWT Verification |  |
|  | (Register/Login)   |   | (Roles & Permissions) |   | (Access + Refresh Rotation) |  |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|                                                                                        |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|  | Centralized DTOs   |   | MapStruct Mappers     |   | Global Exception Handler    |  |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|                                                                                        |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|  | AI Provider Abstr. |   | Media Storage Engine  |   | Scheduled Publishing Engine |  |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|                                                                                        |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
|  | Audit Trail Logger |   | Real-Time Alerts      |   | Analytics & Telemetry       |  |
|  +--------------------+   +-----------------------+   +-----------------------------+  |
+-------------------------------------------+--------------------------------------------+
                                            |
                         +------------------+------------------+
                         |                                     |
                         v                                     v
          +------------------------------+      +-------------------------------+
          | PostgreSQL 16 DB             |      | Redis 7 In-Memory Cache       |
          | Flyway Version-Controlled    |      | (With Fault-Tolerant Fallback)|
          +------------------------------+      +-------------------------------+
```

---

## Phase Status: All Phases (1 through 5) Completed

- **Phase 1: Foundation + Authentication + RBAC** *(COMPLETED & VERIFIED)*
- **Phase 2: Core CMS + Content Workflow + Categories + Tags + Versioning + Search** *(COMPLETED & VERIFIED)*
- **Phase 3: AI Content Intelligence + Token Tracking + Quota Management** *(COMPLETED & VERIFIED)*
- **Phase 4: Enterprise Features + Media Library + Scheduled Publishing + Notifications + Analytics + Audit Logging** *(COMPLETED & VERIFIED)*
- **Phase 5: Production Hardening + Dockerization + CI/CD Pipeline + Bundle Optimization + Operational Runbooks** *(COMPLETED & VERIFIED)*

---

## Production Deployment with Docker Compose

The complete platform can be provisioned in a single command using Docker Compose:

```bash
# 1. Clone repository & configure environment variables
cp .env.example .env

# 2. Build and launch all 4 services (Postgres, Redis, Backend, Frontend)
docker compose up -d --build

# 3. View service status and health checks
docker compose ps
```

### Services & Ports
* **Frontend Web Application (Nginx SPA):** `http://localhost:80`
* **Backend REST API:** `http://localhost:8080/api/v1`
* **Swagger / OpenAPI Documentation:** `http://localhost:8080/swagger-ui.html`
* **Healthcheck Liveness:** `http://localhost:8080/actuator/health/liveness`
* **PostgreSQL Database:** `localhost:5432`
* **Redis Cache:** `localhost:6379`

---

## Local Development Setup

### Prerequisites
* Java 17+ (Eclipse Temurin recommended)
* Apache Maven 3.9+
* Node.js 20+ & npm 10+
* Docker Desktop (for PostgreSQL 16 and Redis 7)

### 1. Start Infrastructure
```bash
docker compose up -d postgres redis
```

### 2. Start Backend
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## Automated Test Suites & Build Verification

### Backend Verification
```bash
cd backend
mvn clean verify
```
* **Tests Executed:** **125** automated unit, integration, and security tests.
* **Failures / Errors:** **0**.
* **Artifact Generated:** `backend/target/ai-powered-content-manager-backend-1.0.0-SNAPSHOT.jar`.

### Frontend Verification
```bash
cd frontend
npx tsc --noEmit
npm run build
```
* **Type Safety:** 0 TypeScript errors.
* **Route Code Splitting:** Lazy-loaded routes via `React.lazy` and `Suspense`.
* **Vendor Splitting:** Chunks split into `vendor-react`, `vendor-antd-core`, `vendor-antd-icons`, `vendor-charts`, and `vendor-query`.

---

## Continuous Integration (CI/CD)

The GitHub Actions pipeline is defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) and executes on all pushes and pull requests to `main` and `master`:
1. **`backend-verify`:** Sets up Java 17, executes `mvn -B clean verify`, and uploads test execution reports.
2. **`frontend-verify`:** Sets up Node.js 20, verifies TypeScript type-safety (`npx tsc --noEmit`), and compiles the optimized production bundle (`npm run build`).
3. **`docker-compose-validate`:** Validates compose file syntax (`docker compose config`) and builds both backend and frontend container images.

---

## Operational Runbook & Production Maintenance

### 1. Database Migrations (Flyway)
* Migrations run automatically on application bootstrap via Flyway.
* Migration scripts are located in `backend/src/main/resources/db/migration/`:
  * `V1__init_auth_rbac.sql`
  * `V2__content_categories_tags_versions.sql`
  * `V3__ai_intelligence_requests_responses.sql`
  * `V4__enterprise_media_notifications_audit.sql`
* In production (`SPRING_PROFILES_ACTIVE=prod`), `hibernate.ddl-auto` is strictly enforced to `validate`.

### 2. Database Backup & Disaster Recovery
```bash
# Create immediate database backup
docker exec -t content_manager_postgres pg_dump -U postgres -d content_manager_db -F c -b -v -f /tmp/backup.dump
docker cp content_manager_postgres:/tmp/backup.dump ./backup_$(date +%Y%m%d_%H%M%S).dump

# Restore from backup file
docker cp ./backup_file.dump content_manager_postgres:/tmp/restore.dump
docker exec -t content_manager_postgres pg_restore -U postgres -d content_manager_db -v -c /tmp/restore.dump
```

### 3. Media Uploads Backup
Media files are stored in the named Docker volume `content_manager_media_uploads`:
```bash
# Backup media uploads volume
docker run --rm -v content_manager_media_uploads:/data -v $(pwd):/backup alpine tar czf /backup/media_backup.tar.gz /data

# Restore media uploads volume
docker run --rm -v content_manager_media_uploads:/data -v $(pwd):/backup alpine tar xzf /backup/media_backup.tar.gz -C /
```

### 4. Rollback Procedure
If a deployment fails:
1. Stop currently running containers:
   ```bash
   docker compose down
   ```
2. Revert Git tag/commit to the previous verified release:
   ```bash
   git checkout <previous-stable-tag>
   ```
3. Restart containers with the previous image version:
   ```bash
   docker compose up -d --build
   ```
4. Verify system health:
   ```bash
   curl -f http://localhost:8080/actuator/health/liveness
   curl -f http://localhost/healthz
   ```

### 5. Troubleshooting Common Issues
* **Redis Offline / Network Failure:** The platform is equipped with `CacheConfig` which intercepts cache connection failures. If Redis is unavailable, requests log a warning and fall back to direct PostgreSQL queries without throwing HTTP 500 errors.
* **Path Traversal Interception:** File uploads and downloads are strictly restricted to the normalized `uploads/` directory; traversal sequences (`../`) throw a `FileStorageException` and are logged in audit records.
* **AI Quota Exceeded:** Daily quotas are configured per role (`ROLE_AUTHOR`: 50, `ROLE_EDITOR`: 200, `ROLE_ADMIN`: 500). If a user reaches their quota, the API responds with `429 Too Many Requests`.

---

## REST API Reference

### Authentication (Phase 1)
| HTTP Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user account | Public |
| `POST` | `/api/v1/auth/login` | Authenticate user & return JWT tokens | Public |
| `POST` | `/api/v1/auth/refresh` | Rotate expired access token with refresh token | Public |
| `POST` | `/api/v1/auth/logout` | Invalidate & revoke refresh token | Public |

### Core CMS & Content Workflow (Phase 2)
| HTTP Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/contents` | Search & filter content with pagination | `CONTENT_READ` |
| `POST` | `/api/v1/contents` | Create draft content with auto-slug | `CONTENT_CREATE` |
| `GET` | `/api/v1/contents/{id}` | Get content by ID | `CONTENT_READ` |
| `PUT` | `/api/v1/contents/{id}` | Update content (auto-snapshots version) | `CONTENT_UPDATE` |
| `DELETE` | `/api/v1/contents/{id}` | Delete content item | `CONTENT_DELETE` |
| `POST` | `/api/v1/contents/{id}/submit` | Transition `DRAFT` $\to$ `IN_REVIEW` | `CONTENT_SUBMIT` |
| `POST` | `/api/v1/contents/{id}/approve` | Transition `IN_REVIEW` $\to$ `APPROVED` | `CONTENT_APPROVE` |
| `POST` | `/api/v1/contents/{id}/reject` | Transition `IN_REVIEW` $\to$ `REJECTED` | `CONTENT_APPROVE` |
| `POST` | `/api/v1/contents/{id}/publish` | Transition `APPROVED` $\to$ `PUBLISHED` | `CONTENT_PUBLISH` |
| `POST` | `/api/v1/contents/{id}/archive` | Transition to `ARCHIVED` | `CONTENT_UPDATE` |
| `GET` | `/api/v1/contents/{id}/versions` | List version snapshot history | `CONTENT_READ` |
| `POST` | `/api/v1/contents/{id}/versions/{v}/restore` | Rollback to specific version | `CONTENT_UPDATE` |
| `GET` | `/api/v1/categories` | List categories hierarchy | `CONTENT_READ` |
| `GET` | `/api/v1/tags` | List tag taxonomy | `CONTENT_READ` |

### AI Content Intelligence (Phase 3)
| HTTP Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/ai/generate` | Generate full article draft with title, excerpt & tags | `AI_GENERATE` |
| `POST` | `/api/v1/ai/rewrite` | Rewrite text applying desired tone and style | `AI_REWRITE` |
| `POST` | `/api/v1/ai/summarize` | Generate executive summary & key takeaways | `AI_SUMMARIZE` |
| `POST` | `/api/v1/ai/titles` | Generate 5 viral/SEO headline suggestions | `AI_GENERATE` |
| `POST` | `/api/v1/ai/seo` | Generate SEO title, meta description, keywords & slug | `AI_SEO` |
| `POST` | `/api/v1/ai/analyze` | Audit readability, structure, SEO completeness & tone | `AI_ANALYZE` |
| `GET` | `/api/v1/ai/usage` | Get current user's daily AI usage quota & limits | Authenticated |
| `GET` | `/api/v1/ai/history` | Audit log of past AI requests and responses | Authenticated |

### Enterprise Features (Phase 4)
| HTTP Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/media` | Upload file (images, PDFs, docs up to 10MB) | `CONTENT_CREATE` |
| `GET` | `/api/v1/media` | Search, filter & paginate media items | `CONTENT_READ` |
| `GET` | `/api/v1/media/{id}` | Get media metadata | `CONTENT_READ` |
| `GET` | `/api/v1/media/files/{filename}` | Serve raw media asset | Public |
| `DELETE` | `/api/v1/media/{id}` | Delete media file and storage record | `CONTENT_DELETE` |
| `POST` | `/api/v1/contents/{id}/schedule` | Schedule content publication with datetime | `CONTENT_PUBLISH` |
| `GET` | `/api/v1/notifications` | List user in-app notifications with unread count | Authenticated |
| `PUT` | `/api/v1/notifications/{id}/read` | Mark individual notification as read | Authenticated |
| `PUT` | `/api/v1/notifications/read-all` | Mark all user notifications as read | Authenticated |
| `GET` | `/api/v1/audit-logs` | Filter and paginate security audit trails | `AUDIT_VIEW` / `ROLE_ADMIN` |
| `GET` | `/api/v1/users` | Admin search, filter, and paginate user directory | `ROLE_ADMIN` |
| `PUT` | `/api/v1/users/{id}/roles` | Update user assigned roles | `ROLE_ADMIN` |
| `PUT` | `/api/v1/users/{id}/status` | Activate or suspend user account | `ROLE_ADMIN` |
| `GET` | `/api/v1/analytics/dashboard` | Content velocity, status, category & author metrics | `CONTENT_READ` |
| `GET` | `/api/v1/analytics/ai` | AI generation volume, token telemetry & response latency | `ROLE_ADMIN` / `ROLE_EDITOR` |
