package com.batu.insights_service.service.impl;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.insights_service.dto.SpendingPerCategoryDTO;
import com.batu.insights_service.dto.SpendingPerCategoryByAccountDTO;
import com.batu.insights_service.dto.SpendingPerCategoryByAccountResponseDTO;
import com.batu.insights_service.dto.SpendingPerCategoryResponseDTO;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.exception.InvalidDateRangeException;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.insights_service.service.TransactionInsightsService;

@Service
public class TransactionInsightsServiceImpl implements TransactionInsightsService {

    private final TransactionInsightsRepository transactionInsightsRepository;

    public TransactionInsightsServiceImpl(TransactionInsightsRepository transactionInsightsRepository) {
        this.transactionInsightsRepository = transactionInsightsRepository;
    }

    @Override
    public SpendingPerCategoryResponseDTO getSpendingByCategory(Date from, Date to, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());

        List<SpendingPerCategoryDTO> categories = transactionInsightsRepository.findByInterval(from, to, userId);
        return new SpendingPerCategoryResponseDTO(userId, categories);
    }

    @Override
    public SpendingPerCategoryByAccountResponseDTO getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId,
            Jwt principal) {

        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        List<SpendingPerCategoryByAccountDTO> categories = transactionInsightsRepository.findByIntervalAndAccount(from, to, userId, accountId);
        return new SpendingPerCategoryByAccountResponseDTO(userId, accountId, categories);

    }

    @Override
    public void save(TransactionInsightRow transactionInsightRow) {
        transactionInsightsRepository.save(transactionInsightRow);
    }

    private void validateDateRange(Date from, Date to) {
        if (from.after(to)) {
            throw new InvalidDateRangeException("'from' date must be before or equal to 'to' date");
        }
    }
}
