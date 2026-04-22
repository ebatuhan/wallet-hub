CREATE TABLE IF NOT EXISTS clickhouse.transactions
(
    date Date,
    primary_category_id UUID,
    payment_channel LowCardinality(String),
    amount Decimal(18, 2),
    is_outflow UInt8,
    is_active UInt8,
    iso_currency_code LowCardinality(String),
    user_id UUID,
    account_id UUID,
    transaction_id UUID,
    updated_at DateTime64(9)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(date)
ORDER BY (account_id, date, transaction_id);

ALTER TABLE clickhouse.transactions ADD COLUMN IF NOT EXISTS primary_category_id UUID;
ALTER TABLE clickhouse.transactions DROP COLUMN IF EXISTS primary_category_code;

ALTER TABLE clickhouse.transactions ADD COLUMN IF NOT EXISTS is_outflow UInt8;
ALTER TABLE clickhouse.transactions ADD COLUMN IF NOT EXISTS is_active UInt8;
ALTER TABLE clickhouse.transactions ADD COLUMN IF NOT EXISTS updated_at DateTime64(9) DEFAULT now64(9);
ALTER TABLE clickhouse.transactions MODIFY COLUMN updated_at DateTime64(9);

CREATE TABLE IF NOT EXISTS clickhouse.account_balance_history
(
    account_id UUID,
    user_id UUID,
    balance Decimal(18, 2),
    iso_currency_code LowCardinality(String),
    date Date,
    updated_at DateTime64(9) DEFAULT now64(9)
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(date)
ORDER BY (account_id, date);

ALTER TABLE clickhouse.account_balance_history MODIFY COLUMN updated_at DateTime64(9);
