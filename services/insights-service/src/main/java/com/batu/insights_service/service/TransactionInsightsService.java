package com.batu.insights_service.service;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

public interface TransactionInsightsService {
    SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(String from, UUID accountId, Jwt principal);
    SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(String from, UUID accountId, UUID userId);
    SpendingPerCategoryResponseDto getSpendingByCategory(String from, Jwt principal);
    SpendingPerCategoryResponseDto getSpendingByCategory(String from, UUID userId);
    SpendingGraphResponseDto getSpendingGraph(String from, Jwt principal);
    SpendingGraphResponseDto getSpendingGraph(String from, UUID userId);
    SpendingGraphResponseDto getSpendingGraphByAccount(String from, UUID accountId, Jwt principal);
    SpendingGraphResponseDto getSpendingGraphByAccount(String from, UUID accountId, UUID userId);
    IncomeSummaryResponseDto getIncome(Date from, Date to, Jwt principal);
    IncomeSummaryResponseDto getIncome(Date from, Date to, UUID userId);

    void save(TransactionInsightRow transactionInsightRow);

    void removeAccountTransactions(UUID accountId, UUID userId);
}
