-- ==============================================================================
-- AI Intelligence: Requests and Responses Tracking Schema
-- ==============================================================================

CREATE TABLE ai_requests (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    user_id BIGINT,
    content_id BIGINT,
    operation VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    request_metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_requests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_requests_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_requests_user_id ON ai_requests(user_id);
CREATE INDEX idx_ai_requests_content_id ON ai_requests(content_id);
CREATE INDEX idx_ai_requests_operation ON ai_requests(operation);
CREATE INDEX idx_ai_requests_created_at ON ai_requests(created_at);

CREATE TABLE ai_responses (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    request_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    execution_time_ms BIGINT,
    response_metadata TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_responses_request FOREIGN KEY (request_id) REFERENCES ai_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_responses_request_id ON ai_responses(request_id);
CREATE INDEX idx_ai_responses_status ON ai_responses(status);
CREATE INDEX idx_ai_responses_created_at ON ai_responses(created_at);
