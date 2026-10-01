-- ==============================================================================
-- V1__init_auth_rbac.sql
-- Initial Schema: Users, Roles, Permissions, User Roles, Role Permissions, Refresh Tokens
-- ==============================================================================

-- 1. Permissions Table
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permissions_name ON permissions(name);

-- 2. Roles Table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roles_name ON roles(name);

-- 3. Role-Permissions Join Table
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- 4. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_uuid ON users(uuid);
CREATE INDEX idx_users_status ON users(status);

-- 5. User-Roles Join Table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- 6. Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ==============================================================================
-- Seed Baseline Roles
-- ==============================================================================
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Platform Administrator with full unrestricted access'),
('ROLE_EDITOR', 'Editor with publishing, scheduling, and approval permissions'),
('ROLE_AUTHOR', 'Content Creator with authoring and submission permissions'),
('ROLE_VIEWER', 'Viewer with read-only access to published content and metrics');

-- ==============================================================================
-- Seed Baseline Permissions
-- ==============================================================================
INSERT INTO permissions (name, description) VALUES
-- Content Permissions
('CONTENT_CREATE', 'Create new draft content'),
('CONTENT_READ', 'Read accessible content'),
('CONTENT_UPDATE', 'Modify existing content'),
('CONTENT_DELETE', 'Delete content items'),
('CONTENT_SUBMIT', 'Submit content for editorial review'),
('CONTENT_APPROVE', 'Approve submitted content'),
('CONTENT_PUBLISH', 'Publish approved content'),
('CONTENT_SCHEDULE', 'Schedule content for automated publication'),

-- AI Intelligence Permissions
('AI_GENERATE', 'Generate content using AI providers'),
('AI_REWRITE', 'Rewrite and improve content using AI'),
('AI_SUMMARIZE', 'Generate summaries using AI'),
('AI_SEO', 'Generate SEO metadata using AI'),
('AI_ANALYZE', 'Analyze content quality and readability using AI'),

-- Operational & Management Permissions
('USER_MANAGE', 'Manage platform users, roles, and access'),
('ANALYTICS_VIEW', 'View platform analytics and reporting metrics'),
('AUDIT_VIEW', 'View security audit logs and system activity'),
('MEDIA_MANAGE', 'Upload and manage media library assets'),
('SETTINGS_MANAGE', 'Configure platform and AI integration settings');

-- ==============================================================================
-- Assign Permissions to Roles
-- ==============================================================================

-- 1. ADMIN: All permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN';

-- 2. EDITOR: Content full workflow, AI operations, Media
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_EDITOR'
AND p.name IN (
    'CONTENT_CREATE', 'CONTENT_READ', 'CONTENT_UPDATE', 'CONTENT_DELETE',
    'CONTENT_SUBMIT', 'CONTENT_APPROVE', 'CONTENT_PUBLISH', 'CONTENT_SCHEDULE',
    'AI_GENERATE', 'AI_REWRITE', 'AI_SUMMARIZE', 'AI_SEO', 'AI_ANALYZE',
    'MEDIA_MANAGE', 'ANALYTICS_VIEW'
);

-- 3. AUTHOR: Content creation, update, submit, AI assistance, Media
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_AUTHOR'
AND p.name IN (
    'CONTENT_CREATE', 'CONTENT_READ', 'CONTENT_UPDATE', 'CONTENT_SUBMIT',
    'AI_GENERATE', 'AI_REWRITE', 'AI_SUMMARIZE', 'AI_SEO',
    'MEDIA_MANAGE'
);

-- 4. VIEWER: Read content and view analytics
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_VIEWER'
AND p.name IN (
    'CONTENT_READ', 'ANALYTICS_VIEW'
);
