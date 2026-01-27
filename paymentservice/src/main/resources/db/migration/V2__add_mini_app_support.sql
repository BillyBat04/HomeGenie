-- ================================================================
-- Flyway Migration V2: Add Mini-App Support for Payment Platform
-- ================================================================
-- Adds mini-app tracking, commission, and platform-wide features
-- Author: HomeGenie Platform Team
-- Date: 2026-01-17
-- ================================================================

-- ============================================
-- 1. Add Mini-App Support Columns
-- ============================================

-- Add mini_app_id column (defaults to 'maintenance' for backward compatibility)
ALTER TABLE payments 
ADD COLUMN mini_app_id VARCHAR(50) NOT NULL DEFAULT 'maintenance';

-- Add commission tracking columns
ALTER TABLE payments 
ADD COLUMN commission DECIMAL(10, 2) DEFAULT 0.00,
ADD COLUMN commission_rate DECIMAL(5, 4) DEFAULT 0.00;

-- Rename request_id to order_id (generic for all mini-apps)
ALTER TABLE payments 
RENAME COLUMN request_id TO order_id;

-- Add metadata JSON column for mini-app specific data
ALTER TABLE payments
ADD COLUMN metadata_json JSONB DEFAULT NULL;

-- ============================================
-- 2. Create Indexes for Performance
-- ============================================

-- Index for mini-app queries
CREATE INDEX idx_payment_mini_app_id ON payments(mini_app_id);

-- Composite index for user + mini-app queries (unified payment history)
CREATE INDEX idx_payment_user_miniapp ON payments(user_id, mini_app_id);

-- Index for order_id (renamed from maintenance_request_id)
CREATE INDEX idx_payment_order_id ON payments(order_id);

-- Index for commission analytics
CREATE INDEX idx_payment_commission ON payments(mini_app_id, status, created_at);

-- ============================================
-- 3. Update Existing Data
-- ============================================

-- Set commission rate for existing maintenance payments (10%)
UPDATE payments 
SET commission_rate = 0.10,
    commission = amount * 0.10
WHERE mini_app_id = 'maintenance' AND status = 'SUCCEEDED';

-- ============================================
-- 4. Comments for Documentation
-- ============================================

COMMENT ON COLUMN payments.mini_app_id IS 'Mini-app identifier (maintenance, marketplace, finance, etc.)';
COMMENT ON COLUMN payments.order_id IS 'Generic order ID from mini-app (maintenance request ID, booking ID, etc.)';
COMMENT ON COLUMN payments.commission IS 'Platform commission amount calculated at payment time';
COMMENT ON COLUMN payments.commission_rate IS 'Commission rate applied (from config)';
COMMENT ON COLUMN payments.metadata_json IS 'Mini-app specific metadata stored as JSON';

-- ============================================
-- 5. Create Updated Indexes
-- ============================================

-- Drop old index on maintenance_request_id (now order_id, already indexed above)
DROP INDEX IF EXISTS idx_payment_maintenance_id;

-- ============================================
-- 6. Validation Constraints
-- ============================================

-- Ensure commission is positive
ALTER TABLE payments 
ADD CONSTRAINT chk_payment_commission_positive 
CHECK (commission >= 0);

-- Ensure commission rate is between 0 and 1 (0% to 100%)
ALTER TABLE payments 
ADD CONSTRAINT chk_payment_commission_rate_range 
CHECK (commission_rate >= 0 AND commission_rate <= 1);

-- ============================================
-- 7. Sample Data for Testing (Optional)
-- ============================================

-- Commented out for production, uncomment for dev/test environments
-- INSERT INTO payments (user_id, order_id, mini_app_id, amount, commission, commission_rate, status, currency, payment_method)
-- VALUES 
-- (1, 'MAINT-TEST-001', 'maintenance', 100.00, 10.00, 0.10, 'SUCCEEDED', 'USD', 'CREDIT_CARD'),
-- (1, 'MKT-TEST-001', 'marketplace', 250.00, 37.50, 0.15, 'SUCCEEDED', 'USD', 'CREDIT_CARD'),
-- (2, 'MAINT-TEST-002', 'maintenance', 150.00, 15.00, 0.10, 'PENDING', 'USD', 'CREDIT_CARD');

-- ============================================
-- Migration Complete
-- ============================================
-- Payment Service is now Payment Platform v2!
-- Next steps:
-- 1. Update Payment entity in Java code
-- 2. Add PaymentPlatformConfig for mini-app settings
-- 3. Implement MiniAppPaymentRequest/Response DTOs
-- 4. Create POST /api/payments/mini-app endpoint
-- ============================================
