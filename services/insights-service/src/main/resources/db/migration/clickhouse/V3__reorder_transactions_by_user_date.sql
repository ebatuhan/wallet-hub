DROP TABLE IF EXISTS clickhouse.transactions_user_date_ordered;
DROP TABLE IF EXISTS clickhouse.transactions_account_order_backup;

CREATE TABLE clickhouse.transactions_user_date_ordered
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
ORDER BY (user_id, date, account_id, transaction_id);

INSERT INTO clickhouse.transactions_user_date_ordered
SELECT
    date,
    primary_category_id,
    payment_channel,
    amount,
    is_outflow,
    is_active,
    iso_currency_code,
    user_id,
    account_id,
    transaction_id,
    updated_at
FROM clickhouse.transactions;

RENAME TABLE
    clickhouse.transactions TO clickhouse.transactions_account_order_backup,
    clickhouse.transactions_user_date_ordered TO clickhouse.transactions;

DROP TABLE clickhouse.transactions_account_order_backup;
