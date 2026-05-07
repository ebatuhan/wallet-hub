package com.batu.insights_service.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.repository.AccountInsightRepository;

@ExtendWith(MockitoExtension.class)
class AccountInsightRepositoryTest {

    private static final UUID USER_ID = UUID.fromString("8a000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("8a000000-0000-0000-0000-000000000002");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AccountInsightRepository repository;

    @Test
    void save_whenRowProvided_shouldInsertBalanceHistoryWithCurrentProjectionColumns() {
        AccountBalanceDataPointRow row = new AccountBalanceDataPointRow(
                ACCOUNT_ID,
                USER_ID,
                new BigDecimal("125.50"),
                "USD",
                LocalDate.of(2026, 4, 17));

        repository.save(row);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq(ACCOUNT_ID),
                eq(USER_ID),
                eq(new BigDecimal("125.50")),
                eq("USD"),
                eq(java.sql.Date.valueOf("2026-04-17")),
                any(java.sql.Timestamp.class));
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO account_balance_history")
                .contains("account_id")
                .contains("user_id")
                .contains("updated_at");
    }

    @Test
    void deleteByAccount_whenCalled_shouldDeleteOnlyMatchingAccountAndUser() {
        repository.deleteByAccount(ACCOUNT_ID, USER_ID);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(ACCOUNT_ID), eq(USER_ID));
        assertThat(sqlCaptor.getValue())
                .contains("DELETE FROM account_balance_history")
                .contains("account_id = ?")
                .contains("user_id = ?");
    }

    @Test
    void getAccountBalanceHistory_whenCalled_shouldQueryArgMaxLatestBalancesInDateOrder() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        List<AccountBalanceDataPointRow> expected = List.of(new AccountBalanceDataPointRow(
                ACCOUNT_ID,
                USER_ID,
                new BigDecimal("125.50"),
                "USD",
                LocalDate.of(2026, 4, 17)));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ACCOUNT_ID), eq(USER_ID),
                eq(java.sql.Date.valueOf(from)), eq(java.sql.Date.valueOf(to))))
                .thenReturn(expected);

        var result = repository.getAccountBalanceHistory(ACCOUNT_ID, USER_ID, from, to);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(ACCOUNT_ID), eq(USER_ID),
                eq(java.sql.Date.valueOf(from)), eq(java.sql.Date.valueOf(to)));
        assertThat(sqlCaptor.getValue())
                .contains("argMax(balance, updated_at)")
                .contains("argMax(iso_currency_code, updated_at)")
                .contains("date BETWEEN ? AND ?")
                .contains("ORDER BY date ASC");
    }
}
