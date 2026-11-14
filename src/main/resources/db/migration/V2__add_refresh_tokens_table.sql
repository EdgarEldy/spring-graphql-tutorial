CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(1024) NOT NULL UNIQUE,
    expires_at TIMESTAMP     NOT NULL,
    created_at TIMESTAMP     NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
