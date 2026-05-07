package com.batu.insights_service.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
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

import com.batu.insights_service.dto.SpendingCategoryAggregate;
import com.batu.insights_service.dto.SpendingGraphAggregate;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;

@ExtendWith(MockitoExtension.class)
class TransactionInsightsRepositoryTest {

    private static final UUID USER_ID = UUID.fromString("89000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("89000000-0000-0000-0000-000000000002");
    private static final UUID TRANSACTION_ID = UUID.fromString("89000000-0000-0000-0000-000000000003");
    private static final UUID CATEGORY_ID = UUID.fromString("89000000-0000-0000-0000-000000000004");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TransactionInsightsRepository repository;

    @Test
    void save_whenRowProvided_shouldInsertCurrentProjectionColumnsWithUpdatedAt() {
        Instant updatedAt = Instant.parse("2026-04-17T10:15:30Z");
        TransactionInsightRow row = new TransactionInsightRow(
                LocalDate.of(2026, 4, 17),
                CATEGORY_ID,
                "in store",
                new BigDecimal("-15.75"),
                true,
                true,
                "USD",
                USER_ID,
                ACCOUNT_ID,
                TRANSACTION_ID,
                updatedAt);

        repository.save(row);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq(Date.valueOf("2026-04-17")),
                eq(CATEGORY_ID),
                eq("in store"),
                eq(new BigDecimal("-15.75")),
                eq(true),
                eq(true),
                eq("USD"),
                eq(USER_ID),
                eq(ACCOUNT_ID),
                eq(TRANSACTION_ID),
                eq(java.sql.Timestamp.from(updatedAt)));
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO clickhouse.transactions")
                .contains("primary_category_id")
                .contains("is_outflow")
                .contains("is_active")
                .contains("updated_at");
    }

    @Test
    void deleteByAccount_whenCalled_shouldDeleteOnlyMatchingAccountAndUser() {
        repository.deleteByAccount(ACCOUNT_ID, USER_ID);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(ACCOUNT_ID), eq(USER_ID));
        assertThat(sqlCaptor.getValue())
                .contains("DELETE FROM clickhouse.transactions")
                .contains("account_id = ?")
                .contains("user_id = ?");
    }

    @Test
    void findByInterval_whenCalled_shouldUseMaterializedSpendingDailyQueryAndParameters() {
        Date from = Date.valueOf("2026-04-01");
        Date to = Date.valueOf("2026-04-30");
        List<SpendingCategoryAggregate> expected = List.of(new SpendingCategoryAggregate(
                "USD",
                CATEGORY_ID,
                new BigDecimal("100.00"),
                new BigDecimal("42.00")));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to))).thenReturn(expected);

        var result = repository.findByInterval(from, to, USER_ID);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to));
        assertThat(sqlCaptor.getValue())
                .contains("WITH category_totals AS")
                .contains("FROM clickhouse.spending_daily")
                .contains("PREWHERE user_id = ?")
                .contains("date BETWEEN ? AND ?")
                .contains("SUM(total_amount) AS total_amount")
                .contains("ORDER BY iso_currency_code ASC, percentage DESC");
    }

    @Test
    void findByIntervalAndAccount_whenCalled_shouldUseAccountScopedMaterializedQueryAndParameters() {
        Date from = Date.valueOf("2026-04-01");
        Date to = Date.valueOf("2026-04-30");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to), eq(ACCOUNT_ID)))
                .thenReturn(List.of());

        repository.findByIntervalAndAccount(from, to, USER_ID, ACCOUNT_ID);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to), eq(ACCOUNT_ID));
        assertThat(sqlCaptor.getValue())
                .contains("AND account_id = ?")
                .contains("FROM clickhouse.spending_daily")
                .contains("GROUP BY iso_currency_code, primary_category_id");
    }

    @Test
    void findIncomeByInterval_whenCalled_shouldUseMaterializedIncomeDailyQueryAndParameters() {
        Date from = Date.valueOf("2026-04-01");
        Date to = Date.valueOf("2026-04-30");
        List<IncomeTotalByCurrencyDto> expected = List.of(new IncomeTotalByCurrencyDto("USD", new BigDecimal("1000.00")));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to))).thenReturn(expected);

        var result = repository.findIncomeByInterval(from, to, USER_ID);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to));
        assertThat(sqlCaptor.getValue())
                .contains("FROM clickhouse.income_daily")
                .contains("PREWHERE user_id = ?")
                .contains("SUM(total_income) AS total_income")
                .contains("ORDER BY iso_currency_code ASC");
    }

    @Test
    void findSpendingGraphByInterval_whenCalled_shouldUseMaterializedSpendingDailyGraphQueryAndParameters() {
        Date from = Date.valueOf("2026-04-01");
        Date to = Date.valueOf("2026-04-30");
        List<SpendingGraphAggregate> expected = List.of(new SpendingGraphAggregate(
                "USD",
                LocalDate.of(2026, 4, 1),
                new BigDecimal("20.00")));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to))).thenReturn(expected);

        var result = repository.findSpendingGraphByInterval(from, to, USER_ID, "toDate(latest_date)");

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to));
        assertThat(sqlCaptor.getValue())
                .contains("toDate(latest_date) AS bucket")
                .contains("FROM clickhouse.spending_daily")
                .contains("SUM(total_amount) AS total_amount")
                .contains("GROUP BY iso_currency_code, bucket");
    }

    @Test
    void findSpendingGraphByIntervalAndAccount_whenCalled_shouldApplyAccountFilterAndParameters() {
        Date from = Date.valueOf("2026-04-01");
        Date to = Date.valueOf("2026-04-30");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to), eq(ACCOUNT_ID)))
                .thenReturn(List.of());

        repository.findSpendingGraphByIntervalAndAccount(from, to, USER_ID, ACCOUNT_ID, "toDate(toStartOfWeek(latest_date))");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(USER_ID), eq(from), eq(to), eq(ACCOUNT_ID));
        assertThat(sqlCaptor.getValue())
                .contains("AND account_id = ?")
                .contains("toDate(toStartOfWeek(latest_date)) AS bucket");
    }
}
