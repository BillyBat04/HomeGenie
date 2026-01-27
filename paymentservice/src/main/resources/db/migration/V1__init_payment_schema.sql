-- ================================================================
-- Flyway Migration V1: Initial Payment Service Schema
-- ================================================================
-- Creates tables: payments, invoices, transactions
-- Author: HomeGenie Team
-- Date: 2026-01-16
-- ================================================================

-- Create payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    maintenance_request_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_charge_id VARCHAR(255),
    payment_method VARCHAR(50),
    description TEXT,
    metadata TEXT,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for payments table
CREATE INDEX idx_payment_user_id ON payments(user_id);
CREATE INDEX idx_payment_maintenance_id ON payments(maintenance_request_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_stripe_intent ON payments(stripe_payment_intent_id);
CREATE INDEX idx_payment_created_at ON payments(created_at);

-- Create invoices table
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    tax DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    due_date DATE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
);

-- Create indexes for invoices table
CREATE INDEX idx_invoice_payment_id ON invoices(payment_id);
CREATE INDEX idx_invoice_user_id ON invoices(user_id);
CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_status ON invoices(status);

-- Create transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    stripe_transaction_id VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- Create indexes for transactions table
CREATE INDEX idx_transaction_payment_id ON transactions(payment_id);
CREATE INDEX idx_transaction_type ON transactions(transaction_type);
CREATE INDEX idx_transaction_status ON transactions(status);

-- ================================================================
-- Comments
-- ================================================================
COMMENT ON TABLE payments IS 'Payment records for maintenance services via Stripe';
COMMENT ON TABLE invoices IS 'Generated invoices for maintenance services';
COMMENT ON TABLE transactions IS 'Transaction log for payment operations';
COMMENT ON COLUMN payments.status IS 'Status: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED';
COMMENT ON COLUMN invoices.status IS 'Status: DRAFT, SENT, PAID, OVERDUE, CANCELLED';
