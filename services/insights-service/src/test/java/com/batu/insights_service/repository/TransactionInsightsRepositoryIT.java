package com.batu.insights_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.batu.insights_service.dto.SpendingCategoryAggregate;
import com.batu.insights_service.dto.SpendingGraphAggregate;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;

class TransactionInsightsRepositoryIT extends ClickHouseRepositoryITSupport {

    private static final UUID USER_ID = UUID.fromString("9a000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("9a000000-0000-0000-0000-000000000099");
    private static final UUID ACCOUNT_ID = UUID.fromString("9a000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("9a000000-0000-0000-0000-000000000003");
    private static final UUID FOOD_CATEGORY_ID = UUID.fromString("9a000000-0000-0000-0000-000000000004");
    private static final UUID TRAVEL_CATEGORY_ID = UUID.fromString("9a000000-0000-0000-0000-000000000005");

    private TransactionInsightsRepository repository;

    @BeforeEach
    void setUpRepository() {
        repository = new TransactionInsightsRepository(jdbcTemplate);
    }

    @Test
    void findByInterval_shouldReadRawSpendingByCurrencyAndCategory() {
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-30.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "-10.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", TRAVEL_CATEGORY_ID, "-60.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-03", FOOD_CATEGORY_ID, "-25.00", true, true, "EUR", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-03", FOOD_CATEGORY_ID, "100.00", false, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-03", FOOD_CATEGORY_ID, "-99.00", true, false, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-03", FOOD_CATEGORY_ID, "-77.00", true, true, "USD", OTHER_USER_ID, ACCOUNT_ID));

        List<SpendingCategoryAggregate> spending = repository.findByInterval(
                date("2026-04-01"), date("2026-04-30"), USER_ID);

        assertThat(spending).hasSize(3);
        assertAggregate(spending.get(0), "EUR", FOOD_CATEGORY_ID, "25.00", "100.00");
        assertAggregate(spending.get(1), "USD", TRAVEL_CATEGORY_ID, "60.00", "60.00");
        assertAggregate(spending.get(2), "USD", FOOD_CATEGORY_ID, "40.00", "40.00");
    }

    @Test
    void findByIntervalAndAccount_shouldRestrictSpendingToRequestedAccount() {
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-30.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-01", TRAVEL_CATEGORY_ID, "-70.00", true, true, "USD", USER_ID, OTHER_ACCOUNT_ID));

        List<SpendingCategoryAggregate> spending = repository.findByIntervalAndAccount(
                date("2026-04-01"), date("2026-04-30"), USER_ID, ACCOUNT_ID);

        assertThat(spending).singleElement().satisfies(aggregate ->
                assertAggregate(aggregate, "USD", FOOD_CATEGORY_ID, "30.00", "100.00"));
    }

    @Test
    void findIncomeByInterval_shouldReadRawIncomeOnly() {
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "1200.00", false, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "300.00", false, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "500.00", false, true, "EUR", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "-80.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "900.00", false, false, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "999.00", false, true, "USD", OTHER_USER_ID, ACCOUNT_ID));

        List<IncomeTotalByCurrencyDto> income = repository.findIncomeByInterval(
                date("2026-04-01"), date("2026-04-30"), USER_ID);

        assertThat(income).hasSize(2);
        assertThat(income.get(0).isoCurrencyCode()).isEqualTo("EUR");
        assertThat(income.get(0).totalIncome()).isEqualByComparingTo("500.00");
        assertThat(income.get(1).isoCurrencyCode()).isEqualTo("USD");
        assertThat(income.get(1).totalIncome()).isEqualByComparingTo("1500.00");
    }

    @Test
    void findSpendingGraphByInterval_shouldBucketAndOrderSpendingByCurrency() {
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-10.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "-20.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-03", FOOD_CATEGORY_ID, "-30.00", true, true, "EUR", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-03", FOOD_CATEGORY_ID, "-90.00", true, true, "USD", OTHER_USER_ID, ACCOUNT_ID));

        List<SpendingGraphAggregate> graph = repository.findSpendingGraphByInterval(
                date("2026-04-01"), date("2026-04-30"), USER_ID, "date");

        assertThat(graph).hasSize(3);
        assertGraphPoint(graph.get(0), "EUR", "2026-04-03", "30.00");
        assertGraphPoint(graph.get(1), "USD", "2026-04-01", "10.00");
        assertGraphPoint(graph.get(2), "USD", "2026-04-02", "20.00");
    }

    @Test
    void deleteByAccount_shouldRemoveOnlyMatchingUserAccountTransactions() {
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-10.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-20.00", true, true, "USD", USER_ID, OTHER_ACCOUNT_ID));
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-30.00", true, true, "USD", OTHER_USER_ID, ACCOUNT_ID));

        repository.deleteByAccount(ACCOUNT_ID, USER_ID);

        Integer remainingMatchingRows = jdbcTemplate.queryForObject(
                "SELECT count() FROM clickhouse.transactions WHERE account_id = ? AND user_id = ?",
                Integer.class,
                ACCOUNT_ID,
                USER_ID);
        Integer totalRows = jdbcTemplate.queryForObject("SELECT count() FROM clickhouse.transactions", Integer.class);
        assertThat(remainingMatchingRows).isZero();
        assertThat(totalRows).isEqualTo(2);
    }

    @Test
    void deleteByAccount_shouldRemoveAccountFromAllTransactionInsightQueries() {
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-10.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", TRAVEL_CATEGORY_ID, "-30.00", true, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "200.00", false, true, "USD", USER_ID, ACCOUNT_ID));
        repository.save(row("2026-04-01", FOOD_CATEGORY_ID, "-50.00", true, true, "USD", USER_ID, OTHER_ACCOUNT_ID));
        repository.save(row("2026-04-02", FOOD_CATEGORY_ID, "500.00", false, true, "USD", USER_ID, OTHER_ACCOUNT_ID));

        repository.deleteByAccount(ACCOUNT_ID, USER_ID);

        assertThat(repository.findByIntervalAndAccount(date("2026-04-01"), date("2026-04-30"), USER_ID, ACCOUNT_ID))
                .isEmpty();
        assertThat(repository.findSpendingGraphByIntervalAndAccount(
                date("2026-04-01"), date("2026-04-30"), USER_ID, ACCOUNT_ID, "date"))
                .isEmpty();

        List<SpendingCategoryAggregate> allSpending = repository.findByInterval(
                date("2026-04-01"), date("2026-04-30"), USER_ID);
        assertThat(allSpending).singleElement().satisfies(aggregate ->
                assertAggregate(aggregate, "USD", FOOD_CATEGORY_ID, "50.00", "100.00"));

        List<SpendingGraphAggregate> allGraph = repository.findSpendingGraphByInterval(
                date("2026-04-01"), date("2026-04-30"), USER_ID, "date");
        assertThat(allGraph).singleElement().satisfies(point ->
                assertGraphPoint(point, "USD", "2026-04-01", "50.00"));

        List<IncomeTotalByCurrencyDto> income = repository.findIncomeByInterval(
                date("2026-04-01"), date("2026-04-30"), USER_ID);
        assertThat(income).singleElement().satisfies(total -> {
            assertThat(total.isoCurrencyCode()).isEqualTo("USD");
            assertThat(total.totalIncome()).isEqualByComparingTo("500.00");
        });
    }

    private static TransactionInsightRow row(String date, UUID categoryId, String amount, boolean isOutflow,
            boolean isActive, String currency, UUID userId, UUID accountId) {
        return new TransactionInsightRow(
                LocalDate.parse(date),
                categoryId,
                "online",
                new BigDecimal(amount),
                isOutflow,
                isActive,
                currency,
                userId,
                accountId,
                UUID.randomUUID(),
                Instant.parse(date + "T12:00:00Z"));
    }

    private static Date date(String value) {
        return Date.valueOf(value);
    }

    private static void assertAggregate(SpendingCategoryAggregate aggregate, String currency, UUID categoryId,
            String totalAmount, String percentage) {
        assertThat(aggregate.isoCurrencyCode()).isEqualTo(currency);
        assertThat(aggregate.primaryCategoryId()).isEqualTo(categoryId);
        assertThat(aggregate.totalAmount()).isEqualByComparingTo(totalAmount);
        assertThat(aggregate.percentage()).isEqualByComparingTo(percentage);
    }

    private static void assertGraphPoint(SpendingGraphAggregate aggregate, String currency, String bucket, String totalAmount) {
        assertThat(aggregate.isoCurrencyCode()).isEqualTo(currency);
        assertThat(aggregate.bucket()).isEqualTo(LocalDate.parse(bucket));
        assertThat(aggregate.totalAmount()).isEqualByComparingTo(totalAmount);
    }
}
