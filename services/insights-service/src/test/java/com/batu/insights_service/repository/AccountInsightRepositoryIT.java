package com.batu.insights_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;

class AccountInsightRepositoryIT extends ClickHouseRepositoryITSupport {

    private static final UUID USER_ID = UUID.fromString("9b000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("9b000000-0000-0000-0000-000000000099");
    private static final UUID ACCOUNT_ID = UUID.fromString("9b000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("9b000000-0000-0000-0000-000000000003");

    private AccountInsightRepository repository;

    @BeforeEach
    void setUpRepository() {
        repository = new AccountInsightRepository(jdbcTemplate);
    }

    @Test
    void getAccountBalanceHistory_shouldReturnLatestBalancePerDateForRequestedAccountAndUser() {
        repository.save(row(ACCOUNT_ID, USER_ID, "100.00", "USD", "2026-04-01"));
        repository.save(row(ACCOUNT_ID, USER_ID, "125.50", "USD", "2026-04-02"));
        repository.save(row(OTHER_ACCOUNT_ID, USER_ID, "999.00", "USD", "2026-04-02"));
        repository.save(row(ACCOUNT_ID, OTHER_USER_ID, "888.00", "USD", "2026-04-02"));
        repository.save(row(ACCOUNT_ID, USER_ID, "140.00", "USD", "2026-05-01"));

        List<AccountBalanceDataPointRow> history = repository.getAccountBalanceHistory(
                ACCOUNT_ID,
                USER_ID,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30));

        assertThat(history).hasSize(2);
        assertPoint(history.get(0), ACCOUNT_ID, USER_ID, "100.00", "USD", "2026-04-01");
        assertPoint(history.get(1), ACCOUNT_ID, USER_ID, "125.50", "USD", "2026-04-02");
    }

    @Test
    void getAccountBalanceHistory_whenNoRowsMatch_shouldReturnEmptyList() {
        repository.save(row(OTHER_ACCOUNT_ID, USER_ID, "999.00", "USD", "2026-04-02"));

        List<AccountBalanceDataPointRow> history = repository.getAccountBalanceHistory(
                ACCOUNT_ID,
                USER_ID,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30));

        assertThat(history).isEmpty();
    }

    @Test
    void deleteByAccount_shouldRemoveOnlyMatchingUserAccountBalanceHistory() {
        repository.save(row(ACCOUNT_ID, USER_ID, "100.00", "USD", "2026-04-01"));
        repository.save(row(OTHER_ACCOUNT_ID, USER_ID, "200.00", "USD", "2026-04-01"));
        repository.save(row(ACCOUNT_ID, OTHER_USER_ID, "300.00", "USD", "2026-04-01"));

        repository.deleteByAccount(ACCOUNT_ID, USER_ID);

        Integer matchingRows = jdbcTemplate.queryForObject(
                "SELECT count() FROM clickhouse.account_balance_history WHERE account_id = ? AND user_id = ?",
                Integer.class,
                ACCOUNT_ID,
                USER_ID);
        Integer totalRows = jdbcTemplate.queryForObject("SELECT count() FROM clickhouse.account_balance_history", Integer.class);
        assertThat(matchingRows).isZero();
        assertThat(totalRows).isEqualTo(2);
    }

    private static AccountBalanceDataPointRow row(UUID accountId, UUID userId, String balance, String currency, String date) {
        return new AccountBalanceDataPointRow(
                accountId,
                userId,
                new BigDecimal(balance),
                currency,
                LocalDate.parse(date));
    }

    private static void assertPoint(AccountBalanceDataPointRow point, UUID accountId, UUID userId, String balance,
            String currency, String date) {
        assertThat(point.accountId()).isEqualTo(accountId);
        assertThat(point.userId()).isEqualTo(userId);
        assertThat(point.balance()).isEqualByComparingTo(balance);
        assertThat(point.isoCurrencyCode()).isEqualTo(currency);
        assertThat(point.date()).isEqualTo(LocalDate.parse(date));
    }
}
