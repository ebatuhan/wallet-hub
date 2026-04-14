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

import com.batu.dashboard_service.client.AccountClient;
import com.batu.dashboard_service.client.BudgetingClient;
import com.batu.dashboard_service.client.InsightsClient;
import com.batu.dashboard_service.client.TransactionClient;
import com.batu.dashboard_service.dto.AccountDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.SpendingCategoryItemDto;
import com.batu.dashboard_service.dto.UserDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.client.BudgetResponseDto;
import com.batu.dashboard_service.dto.client.IncomeSummaryResponseDto;
import com.batu.dashboard_service.dto.client.SpendingPerCategoryByAccountDto;
import com.batu.dashboard_service.dto.client.SpendingPerCategoryByAccountResponseDto;
import com.batu.dashboard_service.dto.client.SpendingPerCategoryDto;
import com.batu.dashboard_service.dto.client.SpendingPerCategoryResponseDto;
import com.batu.dashboard_service.service.DashboardService;
import com.batu.shared.dto.request.PrimaryCategoryIdsRequestDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final InsightsClient insightsClient;
    private final BudgetingClient budgetingClient;

    public DashboardServiceImpl(AccountClient accountClient,
            TransactionClient transactionClient,
            InsightsClient insightsClient,
            BudgetingClient budgetingClient) {
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
        this.insightsClient = insightsClient;
        this.budgetingClient = budgetingClient;
    }

    @Override
    public UserDashboardSummaryResponseDto getUserSummary(LocalDate from, LocalDate to, Integer recentLimit, Jwt principal) {
        LocalDate[] range = resolveRange(from, to);
        int limit = recentLimit == null ? 5 : recentLimit;
        String authorization = bearer(principal);
        String fromParam = range[0].format(ISO_DATE);
        String toParam = range[1].format(ISO_DATE);

        var accountSummary = accountClient.getAccountSummary(authorization).getBody();
        CursorResponse<TransactionViewResponseDto> recentTransactions = transactionClient
                .getTransactions(authorization, null, limit, null)
                .getBody();
        IncomeSummaryResponseDto incomeResponse = insightsClient
                .getIncome(authorization, fromParam, toParam)
                .getBody();
        SpendingPerCategoryResponseDto spendingResponse = insightsClient
                .getSpendingByCategory(authorization, fromParam, toParam)
                .getBody();
        List<BudgetResponseDto> budgets = budgetingClient.getBudgets(authorization).getBody();

        var spendingSection = new UserDashboardSummaryResponseDto.SpendingSectionDto(
                spendingResponse == null ? java.math.BigDecimal.ZERO : spendingResponse.totalSpent(),
                enrichSpending(spendingResponse == null ? List.of() : spendingResponse.categories(), authorization));

        List<BudgetResponseDto> budgetItems = budgets == null ? List.of() : budgets.stream().limit(3).toList();
        long activeBudgetCount = budgets == null ? 0 : budgets.size();
        long overBudgetCount = budgets == null ? 0
                : budgets.stream().filter(budget -> budget.spentAmount().compareTo(budget.limitAmount()) > 0).count();

        return new UserDashboardSummaryResponseDto(
                UUID.fromString(principal.getSubject()),
                new UserDashboardSummaryResponseDto.PeriodDto(range[0], range[1]),
                accountSummary,
                new UserDashboardSummaryResponseDto.IncomeSectionDto(
                        incomeResponse == null ? List.of() : incomeResponse.totalsByCurrency()),
                new UserDashboardSummaryResponseDto.RecentTransactionsDto(
                        recentTransactions == null ? List.of() : recentTransactions.getData(),
                        recentTransactions != null && recentTransactions.isHasMore(),
                        recentTransactions == null ? null : recentTransactions.getNextCursor()),
                spendingSection,
                new UserDashboardSummaryResponseDto.BudgetHighlightsDto(activeBudgetCount, overBudgetCount, budgetItems));
    }

    @Override
    public AccountDashboardSummaryResponseDto getAccountSummary(UUID accountId, LocalDate from, LocalDate to,
            Integer recentLimit, Jwt principal) {
        LocalDate[] range = resolveRange(from, to);
        int limit = recentLimit == null ? 10 : recentLimit;
        String authorization = bearer(principal);
        String fromParam = range[0].format(ISO_DATE);
        String toParam = range[1].format(ISO_DATE);

        var account = accountClient.getAccount(authorization, accountId).getBody();
        var balanceHistory = insightsClient.getAccountBalanceHistory(authorization, accountId, fromParam, toParam).getBody();
        SpendingPerCategoryByAccountResponseDto spendingResponse = insightsClient
                .getSpendingByCategoryByAccount(authorization, accountId, fromParam, toParam)
                .getBody();
        CursorResponse<TransactionViewResponseDto> recentTransactions = transactionClient
                .getTransactions(authorization, accountId, limit, null)
                .getBody();

        var spendingSection = new UserDashboardSummaryResponseDto.SpendingSectionDto(
                spendingResponse == null ? java.math.BigDecimal.ZERO : spendingResponse.totalSpent(),
                enrichAccountSpending(spendingResponse == null ? List.of() : spendingResponse.categories(), authorization));

        return new AccountDashboardSummaryResponseDto(
                UUID.fromString(principal.getSubject()),
                new UserDashboardSummaryResponseDto.PeriodDto(range[0], range[1]),
                account,
                balanceHistory == null ? List.of() : balanceHistory,
                spendingSection,
                new UserDashboardSummaryResponseDto.RecentTransactionsDto(
                        recentTransactions == null ? List.of() : recentTransactions.getData(),
                        recentTransactions != null && recentTransactions.isHasMore(),
                        recentTransactions == null ? null : recentTransactions.getNextCursor()));
    }

    private List<SpendingCategoryItemDto> enrichSpending(List<SpendingPerCategoryDto> categories, String authorization) {
        Map<UUID, TransactionPrimaryCategoryDto> categoryMetadata = loadCategoryMetadata(
                categories.stream().map(SpendingPerCategoryDto::primaryCategoryId).collect(Collectors.toSet()),
                authorization);

        return categories.stream()
                .map(category -> toSpendingItem(category.primaryCategoryId(), category.percentage(), category.totalAmount(),
                        categoryMetadata.get(category.primaryCategoryId())))
                .toList();
    }

    private List<SpendingCategoryItemDto> enrichAccountSpending(List<SpendingPerCategoryByAccountDto> categories,
            String authorization) {
        Map<UUID, TransactionPrimaryCategoryDto> categoryMetadata = loadCategoryMetadata(
                categories.stream().map(SpendingPerCategoryByAccountDto::primaryCategoryId).collect(Collectors.toSet()),
                authorization);

        return categories.stream()
                .map(category -> toSpendingItem(category.primaryCategoryId(), category.percentage(), category.totalAmount(),
                        categoryMetadata.get(category.primaryCategoryId())))
                .toList();
    }

    private Map<UUID, TransactionPrimaryCategoryDto> loadCategoryMetadata(Set<UUID> categoryIds, String authorization) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        List<TransactionPrimaryCategoryDto> categories = transactionClient
                .getPrimaryCategoriesByIds(authorization, new PrimaryCategoryIdsRequestDto(categoryIds))
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

    private String bearer(Jwt principal) {
        return "Bearer " + principal.getTokenValue();
    }
}
