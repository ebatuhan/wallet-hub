CREATE TABLE IF NOT EXISTS clickhouse.transactions
(
    date Date,
    primary_category_code LowCardinality(String),
    payment_channel LowCardinality(String),
    amount Decimal(18, 2),
    iso_currency_code LowCardinality(String),
    user_id UUID,
    account_id UUID,
    transaction_id UUID
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(date)
ORDER BY (account_id, date, transaction_id);

CREATE TABLE IF NOT EXISTS clickhouse.account_balance_history
(
    account_id UUID,
    user_id UUID,
    balance Decimal(18, 2),
    iso_currency_code LowCardinality(String),
    date Date,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(date)
ORDER BY (account_id, date);

