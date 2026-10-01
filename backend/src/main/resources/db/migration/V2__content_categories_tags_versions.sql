-- ==============================================================================
-- V2__content_categories_tags_versions.sql
-- Phase 2: Content, Categories, Tags, Content-Tags, Content Versions
-- ==============================================================================

-- 1. Categories Table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_name ON categories(name);

-- 2. Tags Table
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tags_slug ON tags(slug);
CREATE INDEX idx_tags_name ON tags(name);

-- 3. Contents Table
CREATE TABLE contents (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    title VARCHAR(300) NOT NULL,
    slug VARCHAR(350) NOT NULL,
    description VARCHAR(1000),
    body TEXT,
    excerpt VARCHAR(500),
    content_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    author_id BIGINT NOT NULL,
    category_id BIGINT,
    featured_image VARCHAR(1000),
    seo_title VARCHAR(300),
    seo_description VARCHAR(500),
    published_at TIMESTAMP WITH TIME ZONE,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contents_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_contents_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_contents_uuid ON contents(uuid);
CREATE INDEX idx_contents_slug ON contents(slug);
CREATE INDEX idx_contents_status ON contents(status);
CREATE INDEX idx_contents_content_type ON contents(content_type);
CREATE INDEX idx_contents_author_id ON contents(author_id);
CREATE INDEX idx_contents_category_id ON contents(category_id);
CREATE INDEX idx_contents_published_at ON contents(published_at);
CREATE INDEX idx_contents_scheduled_at ON contents(scheduled_at);
CREATE INDEX idx_contents_created_at ON contents(created_at);
CREATE INDEX idx_contents_title ON contents(title);

-- 4. Content-Tags Join Table (Many-to-Many)
CREATE TABLE content_tags (
    content_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (content_id, tag_id),
    CONSTRAINT fk_content_tags_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_content_tags_content_id ON content_tags(content_id);
CREATE INDEX idx_content_tags_tag_id ON content_tags(tag_id);

-- 5. Content Versions Table
CREATE TABLE content_versions (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(300) NOT NULL,
    body TEXT,
    change_summary VARCHAR(500),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_versions_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_versions_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_content_version UNIQUE (content_id, version_number)
);

CREATE INDEX idx_content_versions_content_id ON content_versions(content_id);
CREATE INDEX idx_content_versions_version_number ON content_versions(version_number);

-- 6. Seed default categories
INSERT INTO categories (name, slug, description) VALUES
('Technology', 'technology', 'Articles about technology, software, and innovation'),
('Business', 'business', 'Business strategies, finance, and entrepreneurship'),
('Marketing', 'marketing', 'Marketing strategies, campaigns, and analytics'),
('Design', 'design', 'UI/UX design, graphic design, and creative content'),
('General', 'general', 'General purpose content and announcements');

-- 7. Seed default tags
INSERT INTO tags (name, slug) VALUES
('Featured', 'featured'),
('Breaking News', 'breaking-news'),
('Tutorial', 'tutorial'),
('Opinion', 'opinion'),
('How-To', 'how-to');
