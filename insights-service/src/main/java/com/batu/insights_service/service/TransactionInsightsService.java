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
import com.batu.shared.messaging.event.TransactionRemoved;

public interface TransactionInsightsService {
    SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId, Jwt principal);
    SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId, UUID userId);
    SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, Jwt principal);
    SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, UUID userId);
    SpendingGraphResponseDto getSpendingGraph(Date from, Date to, Jwt principal);
    SpendingGraphResponseDto getSpendingGraph(Date from, Date to, UUID userId);
    SpendingGraphResponseDto getSpendingGraphByAccount(Date from, Date to, UUID accountId, Jwt principal);
    SpendingGraphResponseDto getSpendingGraphByAccount(Date from, Date to, UUID accountId, UUID userId);
    IncomeSummaryResponseDto getIncome(Date from, Date to, Jwt principal);
    IncomeSummaryResponseDto getIncome(Date from, Date to, UUID userId);

    void save(TransactionInsightRow transactionInsightRow);

    void remove(TransactionRemoved transactionRemoved);
}
