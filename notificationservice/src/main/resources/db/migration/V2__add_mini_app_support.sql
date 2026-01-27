-- ================================================================
-- Flyway Migration V2: Add Mini-App Support for Notification Platform
-- ================================================================
-- Adds mini-app tracking to notifications table
-- Author: HomeGenie Platform Team
-- Date: 2026-01-17
-- ================================================================

-- Add mini_app_id column (defaults to 'maintenance' for backward compatibility)
ALTER TABLE notifications 
ADD COLUMN mini_app_id VARCHAR(50) NOT NULL DEFAULT 'maintenance';

-- Add index for mini-app filtering
CREATE INDEX idx_notification_mini_app_id ON notifications(mini_app_id);

-- Add composite index for user + mini-app queries
CREATE INDEX idx_notification_user_miniapp ON notifications(user_id, mini_app_id);

-- Add index for analytics (mini-app + status + created_at)
CREATE INDEX idx_notification_analytics ON notifications(mini_app_id, status, created_at);

-- Update existing notifications to have 'maintenance' mini_app_id
UPDATE notifications 
SET mini_app_id = 'maintenance' 
WHERE mini_app_id IS NULL OR mini_app_id = '';

-- Add comment
COMMENT ON COLUMN notifications.mini_app_id IS 'Mini-app identifier (maintenance, marketplace, booking, etc.)';
