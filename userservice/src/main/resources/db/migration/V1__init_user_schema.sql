-- ================================================================
-- Flyway Migration V1: Initial User Service Schema
-- ================================================================
-- Creates tables: users, refresh_tokens
-- Author: HomeGenie Team
-- Date: 2026-01-16
-- ================================================================

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    flat_number VARCHAR(50),
    specialty VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'RESIDENT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT true
);

-- Create indexes for users table
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_role_active ON users(role, active);
CREATE INDEX idx_user_specialty ON users(specialty);

-- Create refresh_tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for refresh_tokens table
CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_user_revoked ON refresh_tokens(user_id, revoked);

-- ================================================================
-- Comments
-- ================================================================
COMMENT ON TABLE users IS 'User accounts for residents, technicians, and admins';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';
COMMENT ON COLUMN users.role IS 'User role: RESIDENT, TECHNICIAN, or ADMIN';
COMMENT ON COLUMN users.specialty IS 'Technician specialty area (e.g., plumbing, electrical)';
