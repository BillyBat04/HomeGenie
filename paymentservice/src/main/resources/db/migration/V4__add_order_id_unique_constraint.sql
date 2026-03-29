-- ================================================================
-- Flyway Migration V4: Add Unique Constraint on payments.order_id
-- ================================================================
-- Prevents duplicate payments for the same order when multiple service
-- instances race on check-then-insert (SELECT-then-INSERT anti-pattern).
-- DataIntegrityViolationException is caught in PaymentService and
-- mapped to PaymentAlreadyExistsException (409 Conflict).
-- Author: HomeGenie Platform Team
-- Date: 2026-03-29
-- ================================================================

ALTER TABLE payments ADD CONSTRAINT uq_payments_order_id UNIQUE (order_id);
