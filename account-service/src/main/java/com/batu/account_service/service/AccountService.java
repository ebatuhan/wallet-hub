package com.batu.account_service.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.account_service.CursorResponse;
import com.batu.account_service.dto.AccountInformationRequestDto;
import com.batu.account_service.dto.AccountInformationResponseDto;
import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.dto.AccountViewDto;
import com.batu.account_service.enums.AccountSortField;

public interface AccountService {

    CursorResponse<AccountViewDto> getAccountsViewPaginated(Jwt principal,
            String accountName,
            String institutionId,
            String accountType,
            String accountSubtype,
            String cursor,
            int limit,
            AccountSortField sortBy,
            Sort.Direction direction);

    AccountResponseDto getAccount(UUID accountId, Jwt principal);

    List<AccountInformationResponseDto> getAccountsByGivenIds(AccountInformationRequestDto request);
}
