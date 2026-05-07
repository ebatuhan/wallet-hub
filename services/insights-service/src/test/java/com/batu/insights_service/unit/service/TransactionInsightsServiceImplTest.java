package com.batu.insights_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.batu.insights_service.dto.SpendingCategoryAggregate;
import com.batu.insights_service.dto.SpendingGraphAggregate;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.insights_service.service.impl.TransactionInsightsServiceImpl;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;

@ExtendWith(MockitoExtension.class)
class TransactionInsightsServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("84000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("84000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_FOOD = UUID.fromString("84000000-0000-0000-0000-000000000003");
    private static final UUID CATEGORY_RENT = UUID.fromString("84000000-0000-0000-0000-000000000004");

    @Mock
    private TransactionInsightsRepository transactionInsightsRepository;

    @InjectMocks
    private TransactionInsightsServiceImpl service;

    @Test
    void getSpendingByCategory_whenRepositoryReturnsRows_shouldGroupByCurrencyAndSumTotalsWithoutMutation() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-30");
        when(transactionInsightsRepository.findByInterval(from, to, USER_ID)).thenReturn(List.of(
                spending("USD", CATEGORY_FOOD, "60.00", "120.00"),
                spending("USD", CATEGORY_RENT, "40.00", "80.00"),
                spending("EUR", CATEGORY_FOOD, "100.00", "50.00")));

        var result = service.getSpendingByCategory(from, to, jwt());

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.spendingByCurrency()).hasSize(2);
        assertThat(result.spendingByCurrency().get(0).isoCurrencyCode()).isEqualTo("USD");
        assertThat(result.spendingByCurrency().get(0).totalSpent()).isEqualByComparingTo("200.00");
        assertThat(result.spendingByCurrency().get(0).categoryBreakdown())
                .extracting(category -> category.primaryCategoryId())
                .containsExactly(CATEGORY_FOOD, CATEGORY_RENT);
        assertThat(result.spendingByCurrency().get(0).categoryBreakdown().get(0).percentageOfCurrencySpending())
                .isEqualByComparingTo("60.00");
        assertThat(result.spendingByCurrency().get(1).isoCurrencyCode()).isEqualTo("EUR");
        assertThat(result.spendingByCurrency().get(1).totalSpent()).isEqualByComparingTo("50.00");
    }

    @Test
    void getSpendingByCategory_whenRepositoryReturnsEmptyRows_shouldReturnEmptyGroups() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-30");
        when(transactionInsightsRepository.findByInterval(from, to, USER_ID)).thenReturn(List.of());

        var result = service.getSpendingByCategory(from, to, USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.spendingByCurrency()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidRangeCalls")
    void queryMethods_whenFromIsAfterTo_shouldThrowBadRequest(QueryCall call) {
        Date from = date("2026-05-01");
        Date to = date("2026-04-30");

        assertThatThrownBy(() -> call.invoke(service, from, to))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(400);
    }

    @ParameterizedTest
    @MethodSource("granularityCases")
    void getSpendingGraph_whenDateRangeHitsBoundary_shouldUseExpectedGranularity(
            LocalDate fromDate,
            LocalDate toDate,
            String expectedGroupBy,
            String expectedBucketExpression) {
        Date from = Date.valueOf(fromDate);
        Date to = Date.valueOf(toDate);
        when(transactionInsightsRepository.findSpendingGraphByInterval(from, to, USER_ID, expectedBucketExpression))
                .thenReturn(List.of());

        var result = service.getSpendingGraph(from, to, USER_ID);

        assertThat(result.groupBy()).isEqualTo(expectedGroupBy);
        assertThat(result.from()).isEqualTo(fromDate);
        assertThat(result.to()).isEqualTo(toDate);
        assertThat(result.seriesByCurrency()).isEmpty();
    }

    @Test
    void getSpendingGraph_whenDailyBucketsAreMissing_shouldFillZeroPoints() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-03");
        when(transactionInsightsRepository.findSpendingGraphByInterval(from, to, USER_ID, "toDate(date)"))
                .thenReturn(List.of(graph("USD", "2026-04-02", "25.50")));

        var result = service.getSpendingGraph(from, to, USER_ID);

        assertThat(result.seriesByCurrency()).hasSize(1);
        assertThat(result.seriesByCurrency().get(0).spendingPoints())
                .extracting(point -> point.amountSpent())
                .containsExactly(decimal("0"), decimal("25.50"), decimal("0"));
    }

    @Test
    void getSpendingGraph_whenWeeklyRangeStartsMidWeek_shouldAlignBucketsToMonday() {
        Date from = date("2026-04-01");
        Date to = date("2026-05-15");
        when(transactionInsightsRepository.findSpendingGraphByInterval(from, to, USER_ID, "toDate(toStartOfWeek(date))"))
                .thenReturn(List.of(graph("USD", "2026-03-30", "10.00")));

        var result = service.getSpendingGraph(from, to, USER_ID);

        assertThat(result.groupBy()).isEqualTo("WEEK");
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(0).bucket()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(0).amountSpent()).isEqualByComparingTo("10.00");
    }

    @Test
    void getSpendingGraph_whenMonthlyRangeStartsMidMonth_shouldAlignBucketsToFirstDay() {
        Date from = date("2026-01-15");
        Date to = date("2026-08-01");
        when(transactionInsightsRepository.findSpendingGraphByInterval(from, to, USER_ID, "toDate(toStartOfMonth(date))"))
                .thenReturn(List.of(graph("USD", "2026-01-01", "75.00")));

        var result = service.getSpendingGraph(from, to, USER_ID);

        assertThat(result.groupBy()).isEqualTo("MONTH");
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(0).bucket()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void getSpendingGraph_whenMultipleCurrenciesReturned_shouldSortSeriesByCurrency() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-03");
        when(transactionInsightsRepository.findSpendingGraphByInterval(from, to, USER_ID, "toDate(date)"))
                .thenReturn(List.of(
                        graph("USD", "2026-04-01", "10.00"),
                        graph("EUR", "2026-04-01", "20.00")));

        var result = service.getSpendingGraph(from, to, USER_ID);

        assertThat(result.seriesByCurrency())
                .extracting(series -> series.isoCurrencyCode())
                .containsExactly("EUR", "USD");
    }

    @Test
    void getSpendingGraphByAccount_whenCalled_shouldPassAccountIdAndReturnAccountScopedResponse() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-03");
        when(transactionInsightsRepository.findSpendingGraphByIntervalAndAccount(from, to, USER_ID, ACCOUNT_ID, "toDate(date)"))
                .thenReturn(List.of(graph("USD", "2026-04-01", "10.00")));

        var result = service.getSpendingGraphByAccount(from, to, ACCOUNT_ID, USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.seriesByCurrency()).hasSize(1);
    }

    @Test
    void getSpendingPerCategoryByAccount_whenRowsExist_shouldPassAccountIdAndGroupCurrency() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-30");
        when(transactionInsightsRepository.findByIntervalAndAccount(from, to, USER_ID, ACCOUNT_ID))
                .thenReturn(List.of(spending("USD", CATEGORY_FOOD, "100.00", "42.00")));

        var result = service.getSpendingPerCategoryByAccount(from, to, ACCOUNT_ID, USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.spendingByCurrency().get(0).totalSpent()).isEqualByComparingTo("42.00");
    }

    @Test
    void getIncome_whenRowsExist_shouldDelegateAndPreserveTotals() {
        Date from = date("2026-04-01");
        Date to = date("2026-04-30");
        var income = List.of(new IncomeTotalByCurrencyDto("USD", decimal("1000.00")));
        when(transactionInsightsRepository.findIncomeByInterval(from, to, USER_ID)).thenReturn(income);

        var result = service.getIncome(from, to, jwt());

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.totalsByCurrency()).isSameAs(income);
    }

    @Test
    void save_whenRowProvided_shouldDelegateToRepositoryWithoutMutation() {
        TransactionInsightRow row = row();

        service.save(row);

        verify(transactionInsightsRepository).save(row);
    }

    @Test
    void removeAccountTransactions_whenAccountAndUserProvided_shouldDelegateToRepository() {
        service.removeAccountTransactions(ACCOUNT_ID, USER_ID);

        verify(transactionInsightsRepository).deleteByAccount(ACCOUNT_ID, USER_ID);
    }

    @Test
    void getIncome_whenJwtSubjectMalformed_shouldThrowAndNotQueryRepository() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("not-a-uuid").build();

        assertThatThrownBy(() -> service.getIncome(date("2026-04-01"), date("2026-04-30"), jwt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> granularityCases() {
        return Stream.of(
                Arguments.of(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1), "DAY", "toDate(date)"),
                Arguments.of(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 2), "WEEK", "toDate(toStartOfWeek(date))"),
                Arguments.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 29), "WEEK", "toDate(toStartOfWeek(date))"),
                Arguments.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), "MONTH", "toDate(toStartOfMonth(date))"));
    }

    private static Stream<Arguments> invalidRangeCalls() {
        return Stream.of(
                Arguments.of((QueryCall) (service, from, to) -> service.getSpendingByCategory(from, to, USER_ID)),
                Arguments.of((QueryCall) (service, from, to) -> service.getSpendingGraph(from, to, USER_ID)),
                Arguments.of((QueryCall) (service, from, to) -> service.getSpendingGraphByAccount(from, to, ACCOUNT_ID, USER_ID)),
                Arguments.of((QueryCall) (service, from, to) -> service.getSpendingPerCategoryByAccount(from, to, ACCOUNT_ID, USER_ID)),
                Arguments.of((QueryCall) (service, from, to) -> service.getIncome(from, to, USER_ID)));
    }

    private interface QueryCall {
        Object invoke(TransactionInsightsServiceImpl service, Date from, Date to);
    }

    private static SpendingCategoryAggregate spending(String currency, UUID categoryId, String percentage, String total) {
        return new SpendingCategoryAggregate(currency, categoryId, decimal(percentage), decimal(total));
    }

    private static SpendingGraphAggregate graph(String currency, String bucket, String total) {
        return new SpendingGraphAggregate(currency, LocalDate.parse(bucket), decimal(total));
    }

    private static TransactionInsightRow row() {
        return new TransactionInsightRow(
                LocalDate.of(2026, 4, 17),
                CATEGORY_FOOD,
                "in store",
                decimal("-15.75"),
                true,
                true,
                "USD",
                USER_ID,
                ACCOUNT_ID,
                UUID.fromString("84000000-0000-0000-0000-000000000005"),
                java.time.Instant.parse("2026-04-17T10:15:30Z"));
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID.toString()).build();
    }

    private static Date date(String date) {
        return Date.valueOf(date);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
