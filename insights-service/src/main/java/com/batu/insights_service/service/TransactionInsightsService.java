package com.batu.insights_service.service;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

public interface TransactionInsightsService {
    SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId, Jwt principal);
    SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, Jwt principal);
    IncomeSummaryResponseDto getIncome(Date from, Date to, Jwt principal);

    void save(TransactionInsightRow transactionInsightRow);
}
