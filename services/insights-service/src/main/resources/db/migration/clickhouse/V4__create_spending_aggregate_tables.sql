DROP VIEW IF EXISTS clickhouse.spending_monthly_by_category_mv;
DROP VIEW IF EXISTS clickhouse.monthly_spending_mv;
DROP VIEW IF EXISTS clickhouse.weekly_spending_mv;

DROP TABLE IF EXISTS clickhouse.spending_monthly_by_category;

CREATE TABLE IF NOT EXISTS clickhouse.monthly_spending
(
    user_id UUID,
    account_id UUID,
    month Date,
    iso_currency_code LowCardinality(String),
    primary_category_id UUID,
    total_amount Decimal(38, 2),
    transaction_count UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(month)
ORDER BY (user_id, account_id, month, iso_currency_code, primary_category_id)
SETTINGS index_granularity = 64;

CREATE TABLE IF NOT EXISTS clickhouse.weekly_spending
(
    user_id UUID,
    account_id UUID,
    month Date,
    week_start Date,
    iso_currency_code LowCardinality(String),
    primary_category_id UUID,
    total_amount Decimal(38, 2),
    transaction_count UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(month)
ORDER BY (user_id, account_id, month, week_start, iso_currency_code, primary_category_id)
SETTINGS index_granularity = 64;

INSERT INTO clickhouse.monthly_spending
SELECT
    user_id,
    account_id,
    toStartOfMonth(date) AS month,
    iso_currency_code,
    primary_category_id,
    SUM(abs(amount)) AS total_amount,
    count() AS transaction_count
FROM clickhouse.transactions
WHERE is_active = 1
  AND is_outflow = 1
GROUP BY user_id, account_id, month, iso_currency_code, primary_category_id;

INSERT INTO clickhouse.weekly_spending
SELECT
    user_id,
    account_id,
    toStartOfMonth(date) AS month,
    toMonday(date) AS week_start,
    iso_currency_code,
    primary_category_id,
    SUM(abs(amount)) AS total_amount,
    count() AS transaction_count
FROM clickhouse.transactions
WHERE is_active = 1
  AND is_outflow = 1
GROUP BY user_id, account_id, month, week_start, iso_currency_code, primary_category_id;

CREATE MATERIALIZED VIEW IF NOT EXISTS clickhouse.monthly_spending_mv
TO clickhouse.monthly_spending
AS
SELECT
    user_id,
    account_id,
    toStartOfMonth(date) AS month,
    iso_currency_code,
    primary_category_id,
    SUM(abs(amount)) AS total_amount,
    count() AS transaction_count
FROM clickhouse.transactions
WHERE is_active = 1
  AND is_outflow = 1
GROUP BY user_id, account_id, month, iso_currency_code, primary_category_id;

CREATE MATERIALIZED VIEW IF NOT EXISTS clickhouse.weekly_spending_mv
TO clickhouse.weekly_spending
AS
SELECT
    user_id,
    account_id,
    toStartOfMonth(date) AS month,
    toMonday(date) AS week_start,
    iso_currency_code,
    primary_category_id,
    SUM(abs(amount)) AS total_amount,
    count() AS transaction_count
FROM clickhouse.transactions
WHERE is_active = 1
  AND is_outflow = 1
GROUP BY user_id, account_id, month, week_start, iso_currency_code, primary_category_id;

DROP TABLE IF EXISTS clickhouse.inbox_events;
