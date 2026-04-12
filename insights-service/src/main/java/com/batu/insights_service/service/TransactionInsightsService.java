package com.batu.insights_service.service;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.insights_service.dto.SpendingPerCategoryByAccountDTO;
import com.batu.insights_service.dto.SpendingPerCategoryByAccountResponseDTO;
import com.batu.insights_service.dto.SpendingPerCategoryDTO;
import com.batu.insights_service.dto.SpendingPerCategoryResponseDTO;
import com.batu.insights_service.entity.TransactionInsightRow;

public interface TransactionInsightsService {
    SpendingPerCategoryByAccountResponseDTO getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId, Jwt principal);
    SpendingPerCategoryResponseDTO getSpendingByCategory(Date from, Date to, Jwt principal);

    void save(TransactionInsightRow transactionInsightRow);
}
