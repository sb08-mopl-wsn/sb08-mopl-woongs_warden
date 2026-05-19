ALTER TABLE users
    ADD CONSTRAINT uk_users_social_type_social_id
        UNIQUE (social_type, social_id);