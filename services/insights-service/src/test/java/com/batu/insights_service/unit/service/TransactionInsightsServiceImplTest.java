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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void getSpendingByCategory_whenYearProvided_shouldQueryTwelveMonthlyBucketsAndGroupCurrency() {
        when(transactionInsightsRepository.findSpendingByMonths(date("2026-01-01"), date("2026-12-01"), USER_ID))
                .thenReturn(List.of(
                        spending("USD", CATEGORY_FOOD, "60.00", "120.00"),
                        spending("USD", CATEGORY_RENT, "40.00", "80.00"),
                        spending("EUR", CATEGORY_FOOD, "100.00", "50.00")));

        var result = service.getSpendingByCategory("2026", jwt());

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.spendingByCurrency()).hasSize(2);
        assertThat(result.spendingByCurrency().get(0).isoCurrencyCode()).isEqualTo("USD");
        assertThat(result.spendingByCurrency().get(0).totalSpent()).isEqualByComparingTo("200.00");
        assertThat(result.spendingByCurrency().get(0).categoryBreakdown())
                .extracting(category -> category.primaryCategoryId())
                .containsExactly(CATEGORY_FOOD, CATEGORY_RENT);
        assertThat(result.spendingByCurrency().get(1).isoCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void getSpendingByCategory_whenMonthProvided_shouldQueryOneMonthlyBucket() {
        when(transactionInsightsRepository.findSpendingByMonths(date("2026-04-01"), date("2026-04-01"), USER_ID))
                .thenReturn(List.of());

        var result = service.getSpendingByCategory("2026-04", USER_ID);

        assertThat(result.spendingByCurrency()).isEmpty();
    }

    @Test
    void getSpendingGraph_whenYearProvided_shouldUseMonthlyAggregateAndFillTwelveMonths() {
        when(transactionInsightsRepository.findMonthlySpendingGraph(date("2026-01-01"), date("2026-12-01"), USER_ID))
                .thenReturn(List.of(graph("USD", "2026-02-01", "25.50")));

        var result = service.getSpendingGraph("2026", USER_ID);

        assertThat(result.groupBy()).isEqualTo("MONTH");
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.seriesByCurrency().get(0).spendingPoints()).hasSize(12);
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(0).amountSpent()).isEqualByComparingTo("0");
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(1).amountSpent()).isEqualByComparingTo("25.50");
    }

    @Test
    void getSpendingGraph_whenMonthProvided_shouldUseWeeklyAggregateAndFillWeekBuckets() {
        when(transactionInsightsRepository.findWeeklySpendingGraph(date("2026-04-01"), USER_ID))
                .thenReturn(List.of(graph("USD", "2026-03-30", "10.00")));

        var result = service.getSpendingGraph("2026-04", USER_ID);

        assertThat(result.groupBy()).isEqualTo("WEEK");
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(0).bucket()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(result.seriesByCurrency().get(0).spendingPoints().get(0).amountSpent()).isEqualByComparingTo("10.00");
    }

    @Test
    void getSpendingGraphByAccount_whenCalled_shouldPassAccountIdAndReturnAccountScopedResponse() {
        when(transactionInsightsRepository.findWeeklySpendingGraphByAccount(date("2026-04-01"), USER_ID, ACCOUNT_ID))
                .thenReturn(List.of(graph("USD", "2026-03-30", "10.00")));

        var result = service.getSpendingGraphByAccount("2026-04", ACCOUNT_ID, USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.seriesByCurrency()).hasSize(1);
    }

    @Test
    void getSpendingPerCategoryByAccount_whenRowsExist_shouldPassAccountIdAndGroupCurrency() {
        when(transactionInsightsRepository.findSpendingByMonthsAndAccount(date("2026-04-01"), date("2026-04-01"), USER_ID, ACCOUNT_ID))
                .thenReturn(List.of(spending("USD", CATEGORY_FOOD, "100.00", "42.00")));

        var result = service.getSpendingPerCategoryByAccount("2026-04", ACCOUNT_ID, USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.spendingByCurrency().get(0).totalSpent()).isEqualByComparingTo("42.00");
    }

    @Test
    void spendingMethods_whenFromInvalid_shouldThrowBadRequest() {
        assertThatThrownBy(() -> service.getSpendingByCategory("2026-04-01", USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(400);
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
    void getIncome_whenFromIsAfterTo_shouldThrowBadRequest() {
        assertThatThrownBy(() -> service.getIncome(date("2026-05-01"), date("2026-04-30"), USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(400);
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
