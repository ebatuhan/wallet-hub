package com.batu.insights_service.service;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.insights_service.dto.SpendingPerCategoryByAccountDTO;
import com.batu.insights_service.dto.SpendingPerCategoryDTO;
import com.batu.insights_service.entity.TransactionInsightRow;

public interface TransactionInsightsService {
    List<SpendingPerCategoryByAccountDTO> getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId, Jwt principal);
    List<SpendingPerCategoryDTO> getSpendingByCategory(Date from, Date to, Jwt principal);

    void save(TransactionInsightRow transactionInsightRow);
}
