-- ================================================================
-- Flyway Migration V3: Invoice Sequence for Safe Multi-Instance Number Generation
-- ================================================================
-- Replaces AtomicLong in-memory counter (breaks under horizontal scaling)
-- with a PostgreSQL SEQUENCE. InvoiceService.generateInvoiceNumber()
-- will query nextval('invoice_number_seq') via JDBC.
-- Author: HomeGenie Platform Team
-- Date: 2026-03-29
-- ================================================================

CREATE SEQUENCE IF NOT EXISTS invoice_number_seq
    START WITH 1001
    INCREMENT BY 1
    NO CYCLE;
