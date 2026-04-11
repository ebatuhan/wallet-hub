package com.batu.insights_service.service.impl;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.insights_service.dto.SpendingPerCategoryDTO;
import com.batu.insights_service.dto.SpendingPerCategoryByAccountDTO;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.insights_service.service.TransactionInsightsService;

@Service
public class TransactionInsightsServiceImpl implements TransactionInsightsService {

    private final TransactionInsightsRepository transactionInsightsRepository;

    public TransactionInsightsServiceImpl(TransactionInsightsRepository transactionInsightsRepository) {
        this.transactionInsightsRepository = transactionInsightsRepository;
    }

    @Override
    public List<SpendingPerCategoryDTO> getSpendingByCategory(Date from, Date to, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());

        return transactionInsightsRepository.findByInterval(from, to, userId);
    }

    @Override
    public List<SpendingPerCategoryByAccountDTO> getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId,
            Jwt principal) {

        UUID userId = UUID.fromString(principal.getSubject());
        return transactionInsightsRepository.findByIntervalAndAccount(from, to, userId, accountId);

    }

    @Override
    public void save(TransactionInsightRow transactionInsightRow) {
        transactionInsightsRepository.save(transactionInsightRow);
    }
}