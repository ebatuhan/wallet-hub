package com.batu.insights_service.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.exception.InvalidDateRangeException;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionInsightsServiceImpl implements TransactionInsightsService {

    private final TransactionInsightsRepository transactionInsightsRepository;

    @Override
    public SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());

        List<SpendingPerCategoryDto> categories = transactionInsightsRepository.findByInterval(from, to, userId);
        return new SpendingPerCategoryResponseDto(userId, totalSpent(categories), categories);
    }

    @Override
    public SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId,
            Jwt principal) {

        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        List<SpendingPerCategoryByAccountDto> categories = transactionInsightsRepository.findByIntervalAndAccount(from, to, userId, accountId);
        return new SpendingPerCategoryByAccountResponseDto(userId, accountId, totalSpentByAccount(categories), categories);

    }

    @Override
    public IncomeSummaryResponseDto getIncome(Date from, Date to, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        return new IncomeSummaryResponseDto(userId, transactionInsightsRepository.findIncomeByInterval(from, to, userId));
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

    private BigDecimal totalSpent(List<SpendingPerCategoryDto> categories) {
        return categories.stream()
                .map(SpendingPerCategoryDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalSpentByAccount(List<SpendingPerCategoryByAccountDto> categories) {
        return categories.stream()
                .map(SpendingPerCategoryByAccountDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
