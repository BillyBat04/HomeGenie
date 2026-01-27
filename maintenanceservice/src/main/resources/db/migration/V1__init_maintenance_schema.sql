-- ================================================================
-- Flyway Migration V1: Initial Maintenance Service Schema
-- ================================================================
-- Creates tables: items, maintenance_requests
-- Author: HomeGenie Team
-- Date: 2026-01-16
-- ================================================================

-- Create items table
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    purchase_date DATE,
    warranty_expiry DATE,
    user_id BIGINT NOT NULL,
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for items table
CREATE INDEX idx_item_user_id ON items(user_id);
CREATE INDEX idx_item_category ON items(category);
CREATE INDEX idx_item_warranty ON items(warranty_expiry);

-- Create maintenance_requests table
CREATE TABLE maintenance_requests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    user_id BIGINT NOT NULL,
    assigned_technician_id BIGINT,
    item_id BIGINT,
    scheduled_date TIMESTAMP,
    completed_date TIMESTAMP,
    estimated_cost DECIMAL(10, 2),
    actual_cost DECIMAL(10, 2),
    image_url TEXT,
    video_url TEXT,
    voice_file_url TEXT,
    ai_classification TEXT,
    ai_classification_confidence DECIMAL(5, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL
);

-- Create indexes for maintenance_requests table
CREATE INDEX idx_maintenance_user_id ON maintenance_requests(user_id);
CREATE INDEX idx_maintenance_technician_id ON maintenance_requests(assigned_technician_id);
CREATE INDEX idx_maintenance_status ON maintenance_requests(status);
CREATE INDEX idx_maintenance_priority ON maintenance_requests(priority);
CREATE INDEX idx_maintenance_category ON maintenance_requests(category);
CREATE INDEX idx_maintenance_created_at ON maintenance_requests(created_at);

-- ================================================================
-- Comments
-- ================================================================
COMMENT ON TABLE items IS 'User-owned items that may require maintenance';
COMMENT ON TABLE maintenance_requests IS 'Maintenance service requests';
COMMENT ON COLUMN maintenance_requests.ai_classification IS 'AI-generated classification using HuggingFace model';
COMMENT ON COLUMN maintenance_requests.status IS 'Status: PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN maintenance_requests.priority IS 'Priority: LOW, MEDIUM, HIGH, URGENT';
