-- ================================================================
-- Flyway Migration V1: Initial Notification Service Schema
-- ================================================================
-- Creates table: notifications
-- Author: HomeGenie Team
-- Date: 2026-01-16
-- ================================================================

-- Create notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    metadata TEXT,
    error_message TEXT,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3
);

-- Create indexes for notifications table
CREATE INDEX idx_notification_user_id ON notifications(user_id);
CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_type ON notifications(type);
CREATE INDEX idx_notification_channel ON notifications(channel);
CREATE INDEX idx_notification_created_at ON notifications(created_at);
CREATE INDEX idx_notification_priority ON notifications(priority);
CREATE INDEX idx_notification_sent_at ON notifications(sent_at);

-- ================================================================
-- Comments
-- ================================================================
COMMENT ON TABLE notifications IS 'Notification delivery tracking for email and SMS';
COMMENT ON COLUMN notifications.type IS 'Notification type: MAINTENANCE_CREATED, MAINTENANCE_ASSIGNED, PAYMENT_SUCCESS, etc.';
COMMENT ON COLUMN notifications.channel IS 'Delivery channel: EMAIL, SMS, PUSH';
COMMENT ON COLUMN notifications.status IS 'Status: PENDING, SENT, DELIVERED, FAILED, CANCELLED';
COMMENT ON COLUMN notifications.priority IS 'Priority: LOW, NORMAL, HIGH, URGENT';
