package com.batu.insights_service.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;
import com.batu.shared.dto.response.SpendingGraphPointDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;

@Repository
public class TransactionInsightsRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<TransactionInsightRow> ROW_MAPPER = (rs, rowNum) -> new TransactionInsightRow(
            rs.getDate("date").toLocalDate(),
            UUID.fromString(rs.getString("primary_category_id")),
            rs.getString("payment_channel"),
            rs.getBigDecimal("amount"),
            rs.getBoolean("is_outflow"),
            rs.getBoolean("is_active"),
            rs.getString("iso_currency_code"),
            UUID.fromString(rs.getString("user_id")),
            UUID.fromString(rs.getString("account_id")),
            UUID.fromString(rs.getString("transaction_id")),
            rs.getTimestamp("updated_at").toInstant());

    private static final RowMapper<SpendingPerCategoryDto> SPENDING_PER_CATEGORY_MAPPER = (rs, rowNum) ->
            new SpendingPerCategoryDto(
                    UUID.fromString(rs.getString("primary_category_id")),
                    rs.getBigDecimal("percentage"),
                    rs.getBigDecimal("total_amount"));

    private static final RowMapper<SpendingPerCategoryByAccountDto> SPENDING_PER_CATEGORY_BY_ACCOUNT_MAPPER = (rs, rowNum) ->
            new SpendingPerCategoryByAccountDto(
                    UUID.fromString(rs.getString("primary_category_id")),
                    rs.getBigDecimal("percentage"),
                    rs.getBigDecimal("total_amount"));

    private static final RowMapper<IncomeTotalByCurrencyDto> INCOME_TOTAL_BY_CURRENCY_MAPPER = (rs, rowNum) ->
            new IncomeTotalByCurrencyDto(
                    rs.getString("iso_currency_code"),
                    rs.getBigDecimal("total_income"));

    private static final RowMapper<SpendingGraphPointDto> SPENDING_GRAPH_POINT_MAPPER = (rs, rowNum) ->
            new SpendingGraphPointDto(
                    rs.getDate("bucket").toLocalDate(),
                    rs.getBigDecimal("total_amount"));

    public TransactionInsightsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(TransactionInsightRow row) {
        final String sql = """
                INSERT INTO clickhouse.transactions (
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
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                java.sql.Date.valueOf(row.date()),
                row.primaryCategoryId(),
                row.paymentChannel(),
                row.amount(),
                row.isOutflow(),
                row.isActive(),
                row.isoCurrencyCode(),
                row.userId(),
                row.accountId(),
                row.transactionId(),
                java.sql.Timestamp.from(row.updatedAt()));
    }

    public List<SpendingPerCategoryDto> findByInterval(Date from, Date to, UUID userId) {
        String sql = """
                WITH latest_transactions AS (
                    SELECT
                        transaction_id,
                        argMax(user_id, updated_at) AS latest_user_id,
                        argMax(primary_category_id, updated_at) AS latest_primary_category_id,
                        argMax(amount, updated_at) AS latest_amount,
                        argMax(is_outflow, updated_at) AS latest_is_outflow,
                        argMax(is_active, updated_at) AS latest_is_active
                    FROM clickhouse.transactions
                    PREWHERE user_id = ?
                      AND date BETWEEN ? AND ?
                    GROUP BY transaction_id
                )
                SELECT
                    latest_user_id AS user_id,
                    latest_primary_category_id AS primary_category_id,
                    SUM(abs(latest_amount)) AS total_amount,
                    round((SUM(abs(latest_amount)) * 100.0) / SUM(SUM(abs(latest_amount))) OVER (), 2) AS percentage
                FROM latest_transactions
                WHERE latest_is_active = 1
                  AND latest_is_outflow = 1
                GROUP BY latest_user_id, latest_primary_category_id
                ORDER BY percentage DESC
                """;
        return jdbcTemplate.query(sql, SPENDING_PER_CATEGORY_MAPPER, userId, from, to);
    }

    public List<SpendingPerCategoryByAccountDto> findByIntervalAndAccount(Date from, Date to, UUID userId, UUID accountId) {
        String sql = """
                WITH latest_transactions AS (
                    SELECT
                        transaction_id,
                        argMax(account_id, updated_at) AS latest_account_id,
                        argMax(user_id, updated_at) AS latest_user_id,
                        argMax(primary_category_id, updated_at) AS latest_primary_category_id,
                        argMax(amount, updated_at) AS latest_amount,
                        argMax(is_outflow, updated_at) AS latest_is_outflow,
                        argMax(is_active, updated_at) AS latest_is_active
                    FROM clickhouse.transactions
                    PREWHERE user_id = ?
                      AND date BETWEEN ? AND ?
                      AND account_id = ?
                    GROUP BY transaction_id
                )
                SELECT
                    latest_account_id AS account_id,
                    latest_user_id AS user_id,
                    latest_primary_category_id AS primary_category_id,
                    SUM(abs(latest_amount)) AS total_amount,
                    round((SUM(abs(latest_amount)) * 100.0) / SUM(SUM(abs(latest_amount))) OVER (), 2) AS percentage
                FROM latest_transactions
                WHERE latest_is_active = 1
                  AND latest_is_outflow = 1
                GROUP BY latest_account_id, latest_user_id, latest_primary_category_id
                ORDER BY percentage DESC
                """;
        return jdbcTemplate.query(sql, SPENDING_PER_CATEGORY_BY_ACCOUNT_MAPPER, userId, from, to, accountId);
    }

    public List<IncomeTotalByCurrencyDto> findIncomeByInterval(Date from, Date to, UUID userId) {
        String sql = """
                WITH latest_transactions AS (
                    SELECT
                        transaction_id,
                        argMax(user_id, updated_at) AS latest_user_id,
                        argMax(iso_currency_code, updated_at) AS latest_iso_currency_code,
                        argMax(amount, updated_at) AS latest_amount,
                        argMax(is_outflow, updated_at) AS latest_is_outflow,
                        argMax(is_active, updated_at) AS latest_is_active
                    FROM clickhouse.transactions
                    PREWHERE user_id = ?
                      AND date BETWEEN ? AND ?
                    GROUP BY transaction_id
                )
                SELECT
                    latest_iso_currency_code AS iso_currency_code,
                    SUM(latest_amount) AS total_income
                FROM latest_transactions
                WHERE latest_is_active = 1
                  AND latest_is_outflow = 0
                GROUP BY latest_iso_currency_code
                ORDER BY latest_iso_currency_code ASC
                """;
        return jdbcTemplate.query(sql, INCOME_TOTAL_BY_CURRENCY_MAPPER, userId, from, to);
    }

    public List<SpendingGraphPointDto> findSpendingGraphByInterval(Date from, Date to, UUID userId, String bucketExpression) {
        String sql = spendingGraphQuery(bucketExpression, false);
        return jdbcTemplate.query(sql, SPENDING_GRAPH_POINT_MAPPER, userId, from, to);
    }

    public List<SpendingGraphPointDto> findSpendingGraphByIntervalAndAccount(Date from, Date to, UUID userId, UUID accountId,
            String bucketExpression) {
        String sql = spendingGraphQuery(bucketExpression, true);
        return jdbcTemplate.query(sql, SPENDING_GRAPH_POINT_MAPPER, userId, from, to, accountId);
    }

    private String spendingGraphQuery(String bucketExpression, boolean byAccount) {
        String accountFilter = byAccount ? "AND account_id = ?" : "";

        return String.format("""
                WITH latest_transactions AS (
                    SELECT
                        transaction_id,
                        argMax(account_id, updated_at) AS latest_account_id,
                        argMax(user_id, updated_at) AS latest_user_id,
                        argMax(date, updated_at) AS latest_date,
                        argMax(amount, updated_at) AS latest_amount,
                        argMax(is_outflow, updated_at) AS latest_is_outflow,
                        argMax(is_active, updated_at) AS latest_is_active
                    FROM clickhouse.transactions
                    PREWHERE user_id = ?
                      AND date BETWEEN ? AND ?
                    %s
                    GROUP BY transaction_id
                )
                SELECT
                    %s AS bucket,
                    SUM(abs(latest_amount)) AS total_amount
                FROM latest_transactions
                WHERE latest_is_active = 1
                  AND latest_is_outflow = 1
                GROUP BY bucket
                ORDER BY bucket ASC
                """, accountFilter, bucketExpression);
    }
}
