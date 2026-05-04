package com.batu.insights_service.repository;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class AccountInsightRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountInsightRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(AccountBalanceDataPointRow row) {
        String sql = """
                INSERT INTO account_balance_history (
                    account_id,
                    user_id,
                    balance,
                    iso_currency_code,
                    date,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                row.accountId(),
                row.userId(),
                row.balance(),
                row.isoCurrencyCode(),
                java.sql.Date.valueOf(row.date()),
                java.sql.Timestamp.from(java.time.Instant.now()));
    }

    public void deleteByAccount(UUID accountId, UUID userId) {
        String sql = """
                DELETE FROM account_balance_history
                WHERE account_id = ?
                  AND user_id = ?
                """;

        jdbcTemplate.update(sql, accountId, userId);
    }

  public List<AccountBalanceDataPointRow> getAccountBalanceHistory(
        UUID accountId,
        UUID userId,
        LocalDate from,
        LocalDate to) {

    final String sql = """
            SELECT
                account_id,
                user_id,
                argMax(balance, updated_at) AS balance,
                argMax(iso_currency_code, updated_at) AS iso_currency_code,
                date
            FROM account_balance_history
            WHERE account_id = ?
              AND user_id = ?
              AND date BETWEEN ? AND ?
            GROUP BY account_id, user_id, date
            ORDER BY date ASC
            """;

    return jdbcTemplate.query(
            sql,
            this::mapRow,
            accountId,
            userId,
            java.sql.Date.valueOf(from),
            java.sql.Date.valueOf(to));
}
    private AccountBalanceDataPointRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AccountBalanceDataPointRow(
                rs.getObject("account_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getBigDecimal("balance"),
                rs.getString("iso_currency_code"),
                rs.getObject("date", LocalDate.class));
    }
}
