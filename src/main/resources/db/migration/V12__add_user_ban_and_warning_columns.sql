ALTER TABLE users
    ADD COLUMN warning_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN is_banned BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN banned_at TIMESTAMP NULL;

ALTER TABLE users
    ADD COLUMN ban_expires_at TIMESTAMP NULL;

CREATE INDEX idx_users_ban_expires
    ON users (is_banned, ban_expires_at, is_locked)
    WHERE is_banned = TRUE AND is_locked = FALSE;
