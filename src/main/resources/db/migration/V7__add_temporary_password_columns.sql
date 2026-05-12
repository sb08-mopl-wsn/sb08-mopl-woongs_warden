ALTER TABLE users
    ADD COLUMN temporary_password VARCHAR(255);

ALTER TABLE users
    ADD COLUMN temporary_password_expired_at timestamptz NULL;

ALTER TABLE users
    ADD COLUMN init_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_init_password_state
        CHECK (
            (init_password = FALSE AND temporary_password IS NULL AND temporary_password_expired_at IS NULL)
                OR
            (init_password = TRUE AND temporary_password IS NOT NULL AND temporary_password_expired_at IS NOT NULL)
            );