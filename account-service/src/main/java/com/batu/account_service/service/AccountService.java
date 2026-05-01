package com.batu.account_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.account_service.enums.AccountSortField;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

public interface AccountService {

    CursorResponse<AccountViewDto> getAccountsViewPaginated(Jwt principal,
            String accountName,
            String institutionName,
            String accountType,
            String accountSubtype,
            String cursor,
            int limit,
            AccountSortField sortBy,
            Sort.Direction direction);

    AccountResponseDto getAccount(UUID accountId, Jwt principal);

    AccountResponseDto getAccount(UUID accountId, UUID userId);

    AccountSummaryResponseDto getAccountSummary(Jwt principal);

    AccountSummaryResponseDto getAccountSummary(UUID userId);

    List<AccountNameResponseDto> getAccountsByGivenIds(AccountNameRequestDto request);

    void create(AccountRequestDto request);

    void update(AccountRequestDto request);

    void upsertFromSync(AccountRequestDto request);

    void deactivateFromSync(UUID accountId, long syncVersion);

    void deactivate(UUID accountId);
}
