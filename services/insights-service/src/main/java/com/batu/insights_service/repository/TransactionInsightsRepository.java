package com.batu.insights_service.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.batu.insights_service.dto.SpendingCategoryAggregate;
import com.batu.insights_service.dto.SpendingGraphAggregate;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;

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

    private static final RowMapper<SpendingCategoryAggregate> SPENDING_CATEGORY_AGGREGATE_MAPPER = (rs, rowNum) ->
            new SpendingCategoryAggregate(
                    rs.getString("iso_currency_code"),
                    UUID.fromString(rs.getString("primary_category_id")),
                    rs.getBigDecimal("percentage"),
                    rs.getBigDecimal("total_amount"));

    private static final RowMapper<IncomeTotalByCurrencyDto> INCOME_TOTAL_BY_CURRENCY_MAPPER = (rs, rowNum) ->
            new IncomeTotalByCurrencyDto(
                    rs.getString("iso_currency_code"),
                    rs.getBigDecimal("total_income"));

    private static final RowMapper<SpendingGraphAggregate> SPENDING_GRAPH_AGGREGATE_MAPPER = (rs, rowNum) ->
            new SpendingGraphAggregate(
                    rs.getString("iso_currency_code"),
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

    public void deleteByAccount(UUID accountId, UUID userId) {
        final String sql = """
                DELETE FROM clickhouse.transactions
                WHERE account_id = ?
                  AND user_id = ?
                """;

        jdbcTemplate.update(sql, accountId, userId);
    }

    public List<SpendingCategoryAggregate> findByInterval(Date from, Date to, UUID userId) {
        String sql = """
                WITH category_totals AS (
                    SELECT
                        iso_currency_code,
                        primary_category_id,
                        SUM(total_amount) AS total_amount
                    FROM clickhouse.spending_daily
                    PREWHERE user_id = ?
                      AND date BETWEEN ? AND ?
                    GROUP BY iso_currency_code, primary_category_id
                )
                SELECT
                    iso_currency_code,
                    primary_category_id,
                    total_amount,
                    round((total_amount * 100.0) / SUM(total_amount) OVER (PARTITION BY iso_currency_code), 2) AS percentage
                FROM category_totals
                ORDER BY iso_currency_code ASC, percentage DESC
                """;
        return jdbcTemplate.query(sql, SPENDING_CATEGORY_AGGREGATE_MAPPER, userId, from, to);
    }

    public List<SpendingCategoryAggregate> findByIntervalAndAccount(Date from, Date to, UUID userId, UUID accountId) {
        String sql = """
                WITH category_totals AS (
                    SELECT
                        iso_currency_code,
                        primary_category_id,
                        SUM(total_amount) AS total_amount
                    FROM clickhouse.spending_daily
                    PREWHERE user_id = ?
                      AND date BETWEEN ? AND ?
                      AND account_id = ?
                    GROUP BY iso_currency_code, primary_category_id
                )
                SELECT
                    iso_currency_code,
                    primary_category_id,
                    total_amount,
                    round((total_amount * 100.0) / SUM(total_amount) OVER (PARTITION BY iso_currency_code), 2) AS percentage
                FROM category_totals
                ORDER BY iso_currency_code ASC, percentage DESC
                """;
        return jdbcTemplate.query(sql, SPENDING_CATEGORY_AGGREGATE_MAPPER, userId, from, to, accountId);
    }

    public List<IncomeTotalByCurrencyDto> findIncomeByInterval(Date from, Date to, UUID userId) {
        String sql = """
                SELECT
                    iso_currency_code,
                    SUM(total_income) AS total_income
                FROM clickhouse.income_daily
                PREWHERE user_id = ?
                  AND date BETWEEN ? AND ?
                GROUP BY iso_currency_code
                ORDER BY iso_currency_code ASC
                """;
        return jdbcTemplate.query(sql, INCOME_TOTAL_BY_CURRENCY_MAPPER, userId, from, to);
    }

    public List<SpendingGraphAggregate> findSpendingGraphByInterval(Date from, Date to, UUID userId, String bucketExpression) {
        String sql = spendingGraphQuery(bucketExpression, false);
        return jdbcTemplate.query(sql, SPENDING_GRAPH_AGGREGATE_MAPPER, userId, from, to);
    }

    public List<SpendingGraphAggregate> findSpendingGraphByIntervalAndAccount(Date from, Date to, UUID userId, UUID accountId,
            String bucketExpression) {
        String sql = spendingGraphQuery(bucketExpression, true);
        return jdbcTemplate.query(sql, SPENDING_GRAPH_AGGREGATE_MAPPER, userId, from, to, accountId);
    }

    private String spendingGraphQuery(String bucketExpression, boolean byAccount) {
        String accountFilter = byAccount ? "AND account_id = ?" : "";

        return String.format("""
                SELECT
                    iso_currency_code,
                    %s AS bucket,
                    SUM(total_amount) AS total_amount
                FROM clickhouse.spending_daily
                PREWHERE user_id = ?
                  AND date BETWEEN ? AND ?
                %s
                GROUP BY iso_currency_code, bucket
                ORDER BY iso_currency_code ASC, bucket ASC
                """, bucketExpression, accountFilter);
    }
}
