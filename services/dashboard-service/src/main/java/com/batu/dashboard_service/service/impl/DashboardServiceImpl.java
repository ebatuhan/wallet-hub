package com.batu.dashboard_service.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import io.micrometer.observation.annotation.Observed;

import com.batu.dashboard_service.client.AccountClient;
import com.batu.dashboard_service.client.BudgetingClient;
import com.batu.dashboard_service.client.InsightsClient;
import com.batu.dashboard_service.client.TransactionClient;
import com.batu.dashboard_service.dto.AccountDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.SpendingCategoryItemDto;
import com.batu.dashboard_service.dto.TransactionDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.UserDashboardSummaryResponseDto;
import com.batu.dashboard_service.service.DashboardService;
import com.batu.shared.dto.request.PrimaryCategoryIdsRequestDto;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingCurrencyGroupDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final InsightsClient insightsClient;
    private final BudgetingClient budgetingClient;

    @Override
    @Observed(name = "dashboard.aggregate.summary", contextualName = "dashboard aggregate summary")
    public UserDashboardSummaryResponseDto getUserSummary(LocalDate from, LocalDate to, Integer recentLimit, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        LocalDate[] range = resolveRange(from, to);
        int limit = recentLimit == null ? 5 : recentLimit;
        String fromParam = range[0].format(ISO_DATE);
        String toParam = range[1].format(ISO_DATE);
        LocalDate[] yearlyRange = resolveYearRange(range[1]);
        String yearlyFromParam = yearlyRange[0].format(ISO_DATE);
        String yearlyToParam = yearlyRange[1].format(ISO_DATE);

        var accountSummary = accountClient.getAccountSummary().getBody();
        CursorResponse<TransactionViewResponseDto> recentTransactions = transactionClient
                .getTransactions(null, limit, null)
                .getBody();
        IncomeSummaryResponseDto incomeResponse = insightsClient.getIncome(fromParam, toParam).getBody();
        SpendingPerCategoryResponseDto spendingResponse = insightsClient.getSpendingByCategory(fromParam, toParam).getBody();
        SpendingGraphResponseDto yearlyTrendByCurrency = insightsClient.getSpendingGraph(yearlyFromParam, yearlyToParam).getBody();

        var spendingSection = new UserDashboardSummaryResponseDto.SpendingSectionDto(
                enrichSpendingGroups(spendingResponse == null ? List.of() : spendingResponse.spendingByCurrency()),
                yearlyTrendByCurrency);

        return new UserDashboardSummaryResponseDto(
                userId,
                new UserDashboardSummaryResponseDto.PeriodDto(range[0], range[1]),
                accountSummary,
                new UserDashboardSummaryResponseDto.IncomeSectionDto(
                        incomeResponse == null ? List.of() : incomeResponse.totalsByCurrency()),
                new UserDashboardSummaryResponseDto.RecentTransactionsDto(
                        recentTransactions == null ? List.of() : recentTransactions.getData(),
                        recentTransactions != null && recentTransactions.isHasMore(),
                        recentTransactions == null ? null : recentTransactions.getNextCursor()),
                spendingSection);
    }

    @Override
    @Observed(name = "dashboard.aggregate.budgets", contextualName = "dashboard aggregate budgets")
    public CursorResponse<BudgetResponseDto> getBudgets(Integer limit, String cursor, String sortBy, String direction,
            Jwt principal) {
        return getEnrichedBudgets(limit, cursor, sortBy, direction);
    }

    @Override
    @Observed(name = "dashboard.aggregate.account-summary", contextualName = "dashboard aggregate account summary")
    public AccountDashboardSummaryResponseDto getAccountSummary(UUID accountId, LocalDate from, LocalDate to,
            Integer limit, String cursor, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        LocalDate[] range = resolveRange(from, to);
        int transactionLimit = limit == null ? 10 : limit;
        String fromParam = range[0].format(ISO_DATE);
        String toParam = range[1].format(ISO_DATE);

        var account = accountClient.getAccount(accountId).getBody();
        var balanceHistory = insightsClient.getAccountBalanceHistory(accountId, fromParam, toParam).getBody();
        SpendingPerCategoryByAccountResponseDto spendingResponse = insightsClient
                .getSpendingByCategoryByAccount(accountId, fromParam, toParam)
                .getBody();
        SpendingGraphResponseDto spendingGraph = insightsClient
                .getSpendingGraphByAccount(accountId, fromParam, toParam)
                .getBody();
        CursorResponse<TransactionViewResponseDto> transactions = transactionClient
                .getTransactions(accountId, transactionLimit, cursor)
                .getBody();

        var spendingSection = new AccountDashboardSummaryResponseDto.SpendingSectionDto(
                enrichSpendingGroups(spendingResponse == null ? List.of() : spendingResponse.spendingByCurrency()),
                spendingGraph);

        return new AccountDashboardSummaryResponseDto(
                userId,
                new UserDashboardSummaryResponseDto.PeriodDto(range[0], range[1]),
                account,
                balanceHistory == null ? List.of() : balanceHistory,
                spendingSection,
                new UserDashboardSummaryResponseDto.RecentTransactionsDto(
                        transactions == null ? List.of() : transactions.getData(),
                        transactions != null && transactions.isHasMore(),
                        transactions == null ? null : transactions.getNextCursor()));
    }

    @Override
    @Observed(name = "dashboard.aggregate.transaction-summary", contextualName = "dashboard aggregate transaction summary")
    public TransactionDashboardSummaryResponseDto getTransactionSummary(UUID transactionId, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        TransactionDto transaction = transactionClient.getTransaction(transactionId).getBody();

        if (transaction == null) {
            return new TransactionDashboardSummaryResponseDto(userId, null, null);
        }

        var account = accountClient.getAccount(transaction.getAccountId()).getBody();

        return new TransactionDashboardSummaryResponseDto(userId, transaction, account);
    }

    private List<UserDashboardSummaryResponseDto.SpendingCurrencyGroupDto> enrichSpendingGroups(
            List<SpendingCurrencyGroupDto> groups) {
        Set<UUID> categoryIds = groups.stream()
                .flatMap(group -> group.categoryBreakdown().stream())
                .map(SpendingPerCategoryDto::primaryCategoryId)
                .collect(Collectors.toSet());
        Map<UUID, TransactionPrimaryCategoryDto> categoryMetadata = loadCategoryMetadata(categoryIds);

        return groups.stream()
                .map(group -> new UserDashboardSummaryResponseDto.SpendingCurrencyGroupDto(
                        group.isoCurrencyCode(),
                        group.totalSpent(),
                        enrichSpending(group.categoryBreakdown(), categoryMetadata)))
                .toList();
    }

    private List<SpendingCategoryItemDto> enrichSpending(List<SpendingPerCategoryDto> categories,
            Map<UUID, TransactionPrimaryCategoryDto> categoryMetadata) {
        return categories.stream()
                .map(category -> toSpendingItem(category.primaryCategoryId(), category.percentageOfCurrencySpending(), category.amountSpent(),
                        categoryMetadata.get(category.primaryCategoryId())))
                .toList();
    }

    private CursorResponse<BudgetResponseDto> getEnrichedBudgets(Integer limit, String cursor, String sortBy,
            String direction) {
        CursorResponse<BudgetResponseDto> budgetResponse = budgetingClient.getBudgets(cursor, limit, sortBy, direction).getBody();
        if (budgetResponse == null || budgetResponse.getData().isEmpty()) {
            return new CursorResponse<>(List.of(), false, null);
        }

        List<BudgetResponseDto> budgets = budgetResponse.getData();

        Map<UUID, TransactionPrimaryCategoryDto> categoryMetadata = loadCategoryMetadata(
                budgets.stream().map(BudgetResponseDto::categoryId).collect(Collectors.toSet()));

        List<BudgetResponseDto> enrichedBudgets = budgets.stream()
                .map(budget -> enrichBudget(budget, categoryMetadata.get(budget.categoryId())))
                .toList();

        return new CursorResponse<>(enrichedBudgets, budgetResponse.isHasMore(), budgetResponse.getNextCursor());
    }

    private BudgetResponseDto enrichBudget(BudgetResponseDto budget, TransactionPrimaryCategoryDto metadata) {
        return new BudgetResponseDto(
                budget.id(),
                budget.categoryId(),
                metadata == null ? null : metadata.getCategoryCode(),
                metadata == null ? null : metadata.getDisplayName(),
                metadata == null ? null : metadata.getIconUrl(),
                budget.limitAmount(),
                budget.spentAmount(),
                budget.isoCurrencyCode(),
                budget.period(),
                budget.periodStart(),
                budget.periodEnd(),
                budget.active());
    }

    private Map<UUID, TransactionPrimaryCategoryDto> loadCategoryMetadata(Set<UUID> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        List<TransactionPrimaryCategoryDto> categories = transactionClient
                .getPrimaryCategoriesByIds(new PrimaryCategoryIdsRequestDto(categoryIds))
                .getBody();

        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }

        Map<UUID, TransactionPrimaryCategoryDto> metadata = new HashMap<>();
        for (TransactionPrimaryCategoryDto category : categories) {
            metadata.put(category.getTransactionPrimaryCategoryId(), category);
        }
        return metadata;
    }

    private SpendingCategoryItemDto toSpendingItem(UUID primaryCategoryId,
            java.math.BigDecimal percentage,
            java.math.BigDecimal totalAmount,
            TransactionPrimaryCategoryDto metadata) {
        return new SpendingCategoryItemDto(
                primaryCategoryId,
                metadata == null ? null : metadata.getCategoryCode(),
                metadata == null ? null : metadata.getDisplayName(),
                metadata == null ? null : metadata.getIconUrl(),
                percentage,
                totalAmount);
    }

    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedFrom = from == null ? today.withDayOfMonth(1) : from;
        LocalDate resolvedTo = to == null ? today : to;
        return new LocalDate[] { resolvedFrom, resolvedTo };
    }

    private LocalDate[] resolveYearRange(LocalDate date) {
        return new LocalDate[] { date.withDayOfYear(1), date.withMonth(12).withDayOfMonth(31) };
    }
}
