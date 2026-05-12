ALTER TABLE users
    ADD COLUMN temporary_password  VARCHAR(255);

ALTER TABLE users
    ADD COLUMN temporary_password_expired_at TIMESTAMPTZ NULL;

ALTER TABLE users
    ADD COLUMN init_password BOOLEAN DEFAULT FALSE;