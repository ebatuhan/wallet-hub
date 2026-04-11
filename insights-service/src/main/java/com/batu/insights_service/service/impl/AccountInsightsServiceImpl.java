package com.batu.insights_service.service.impl;

import com.batu.insights_service.dto.AccountBalanceDataPointDTO;
import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.repository.AccountInsightRepository;
import com.batu.insights_service.service.AccountInsightsService;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AccountInsightsServiceImpl implements AccountInsightsService {

    private final AccountInsightRepository accountInsightRepository;

    public AccountInsightsServiceImpl(AccountInsightRepository accountInsightRepository) {
        this.accountInsightRepository = accountInsightRepository;
    }

    @Override
    public void save(AccountBalanceDataPointRow row) {
        accountInsightRepository.save(row);
    }

    @Override
    public List<AccountBalanceDataPointDTO> getAccountBalanceHistory(UUID accountId, LocalDate from, LocalDate to, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());

        return accountInsightRepository.getAccountBalanceHistory(accountId, userId, from, to)
                .stream()
                .map(row -> new AccountBalanceDataPointDTO(
                        row.accountId(),
                        row.userId(),
                        row.balance(),
                        row.isoCurrencyCode(),
                        row.date()
                ))
                .toList();
    }
}