-- ================================================================
-- Flyway Migration V2: Remove auth responsibilities from userservice
--
-- Identity-service is now the sole authentication authority.
-- password column is no longer required here (credentials live in identity-service).
-- phone_number made optional to accept profiles where it was not collected at signup.
-- refresh_tokens table is retained but unused (safe to keep; will be cleaned in V3).
-- ================================================================

ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ALTER COLUMN phone_number DROP NOT NULL;
