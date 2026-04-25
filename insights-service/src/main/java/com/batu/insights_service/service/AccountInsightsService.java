package com.batu.insights_service.service;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.shared.dto.response.AccountBalanceDataPointDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public interface AccountInsightsService {

    void save(AccountBalanceDataPointRow row);

    List<AccountBalanceDataPointDto> getAccountBalanceHistory(UUID accountId, LocalDate from, LocalDate to, Jwt principal);

    List<AccountBalanceDataPointDto> getAccountBalanceHistory(UUID accountId, LocalDate from, LocalDate to, UUID userId);
}
