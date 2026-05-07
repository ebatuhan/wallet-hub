package com.batu.insights_service.service.impl;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.repository.AccountInsightRepository;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.messaging.event.AccountRemoved;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountInsightsServiceImpl implements AccountInsightsService {

    private final AccountInsightRepository accountInsightRepository;

    @Override
    public void save(AccountBalanceDataPointRow row) {
        accountInsightRepository.save(row);
    }

    @Override
    public void remove(AccountRemoved accountRemoved) {
        accountInsightRepository.deleteByAccount(accountRemoved.getAccountId(), accountRemoved.getUserId());
    }

    @Override
    public List<AccountBalanceDataPointDto> getAccountBalanceHistory(UUID accountId, LocalDate from, LocalDate to, Jwt principal) {
        return getAccountBalanceHistory(accountId, from, to, UUID.fromString(principal.getSubject()));
    }

    @Override
    public List<AccountBalanceDataPointDto> getAccountBalanceHistory(UUID accountId, LocalDate from, LocalDate to, UUID userId) {
        return accountInsightRepository.getAccountBalanceHistory(accountId, userId, from, to)
                .stream()
                .map(row -> new AccountBalanceDataPointDto(
                        row.accountId(),
                        row.userId(),
                        row.balance(),
                        row.isoCurrencyCode(),
                        row.date()
                ))
                .toList();
    }
}
