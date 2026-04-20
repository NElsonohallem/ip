ALTER TABLE password_reset_codes
    ADD COLUMN user_id BIGINT;

ALTER TABLE password_reset_codes
    ADD CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id)
            REFERENCES users(id);