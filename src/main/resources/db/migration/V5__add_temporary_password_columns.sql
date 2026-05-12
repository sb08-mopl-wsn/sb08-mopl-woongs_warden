ALTER TABLE users
    ADD COLUMN temporary_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN temporary_password_expired_at TIMESTAMPTZ NULL;