-- ================================================================
-- Flyway Migration V5: Wallet Feature
-- ================================================================
-- Adds wallets table and wallet_transactions table to support
-- in-app wallet top-up, balance, debit, and transfer flows.
-- Author: HomeGenie Platform Team
-- Date: 2026-03-29
-- ================================================================

CREATE TABLE wallets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE,
    balance     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'USD',
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wallet_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_wallet_user_id ON wallets(user_id);

CREATE TABLE wallet_transactions (
    id                 BIGSERIAL PRIMARY KEY,
    wallet_id          BIGINT        NOT NULL,
    transaction_type   VARCHAR(50)   NOT NULL,
    amount             DECIMAL(12, 2) NOT NULL,
    balance_after      DECIMAL(12, 2) NOT NULL,
    reference_id       VARCHAR(255),
    description        TEXT,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT ck_wallet_tx_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_wallet_tx_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_tx_created_at ON wallet_transactions(created_at);

COMMENT ON TABLE wallets              IS 'In-app wallet per user for HomeGenie platform';
COMMENT ON TABLE wallet_transactions  IS 'Audit log of every wallet credit/debit operation';
