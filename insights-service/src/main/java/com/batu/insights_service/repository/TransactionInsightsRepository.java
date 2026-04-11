package com.batu.insights_service.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.batu.insights_service.dto.SpendingPerCategoryByAccountDTO;
import com.batu.insights_service.dto.SpendingPerCategoryDTO;
import com.batu.insights_service.entity.TransactionInsightRow;

@Repository
public class TransactionInsightsRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<TransactionInsightRow> ROW_MAPPER = (rs, rowNum) -> new TransactionInsightRow(
            rs.getDate("date").toLocalDate(),
            rs.getString("primary_category_code"),
            rs.getString("payment_channel"),
            rs.getBigDecimal("amount"),
            rs.getString("iso_currency_code"),
            UUID.fromString(rs.getString("user_id")),
            UUID.fromString(rs.getString("account_id")),
            UUID.fromString(rs.getString("transaction_id")));

    private static final RowMapper<SpendingPerCategoryDTO> SPENDING_PER_CATEGORY_MAPPER = (rs, rowNum) ->
            new SpendingPerCategoryDTO(
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("primary_category_code"),
                    rs.getBigDecimal("percentage"),
                    rs.getBigDecimal("total_amount"));

    private static final RowMapper<SpendingPerCategoryByAccountDTO> SPENDING_PER_CATEGORY_BY_ACCOUNT_MAPPER = (rs, rowNum) ->
            new SpendingPerCategoryByAccountDTO(
                    UUID.fromString(rs.getString("account_id")),
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("primary_category_code"),
                    rs.getBigDecimal("percentage"),
                    rs.getBigDecimal("total_amount"));

    public TransactionInsightsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(TransactionInsightRow row) {
        final String sql = """
                INSERT INTO clickhouse.transactions (
                    date,
                    primary_category_code,
                    payment_channel,
                    amount,
                    iso_currency_code,
                    user_id,
                    account_id,
                    transaction_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                java.sql.Date.valueOf(row.date()),
                row.primaryCategoryCode(),
                row.paymentChannel(),
                row.amount(),
                row.isoCurrencyCode(),
                row.userId(),
                row.accountId(),
                row.transactionId());
    }

    public List<SpendingPerCategoryDTO> findByInterval(Date from, Date to, UUID userId) {
        String sql = """
                SELECT
                    user_id,
                    primary_category_code,
                    SUM(amount) AS total_amount,
                    round((SUM(amount) * 100.0) / SUM(SUM(amount)) OVER (), 2) AS percentage
                FROM clickhouse.transactions
                WHERE user_id = ?
                  AND date BETWEEN ? AND ?
                GROUP BY user_id, primary_category_code
                ORDER BY percentage DESC
                """;
        return jdbcTemplate.query(sql, SPENDING_PER_CATEGORY_MAPPER, userId, from, to);
    }

    public List<SpendingPerCategoryByAccountDTO> findByIntervalAndAccount(Date from, Date to, UUID userId, UUID accountId) {
        String sql = """
                SELECT
                    account_id,
                    user_id,
                    primary_category_code,
                    SUM(amount) AS total_amount,
                    round((SUM(amount) * 100.0) / SUM(SUM(amount)) OVER (), 2) AS percentage
                FROM clickhouse.transactions
                WHERE user_id = ?
                  AND date BETWEEN ? AND ?
                  AND account_id = ?
                GROUP BY account_id, user_id, primary_category_code
                ORDER BY percentage DESC
                """;
        return jdbcTemplate.query(sql, SPENDING_PER_CATEGORY_BY_ACCOUNT_MAPPER, userId, from, to, accountId);
    }
}