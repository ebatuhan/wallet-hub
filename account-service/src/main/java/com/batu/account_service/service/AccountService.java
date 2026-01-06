package com.batu.account_service.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.account_service.CursorResponse;
import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.dto.AccountViewDto;
import com.batu.account_service.enums.AccountSortField;
import com.batu.shared.dto.AccountNameRequestDto;
import com.batu.shared.dto.AccountNameResponseDto;
import com.batu.shared.dto.AccountsUpsertRequestDto;
import com.batu.shared.dto.AccountsUpsertResponseDto;

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

    List<AccountNameResponseDto> getAccountsByGivenIds(AccountNameRequestDto request);

    AccountsUpsertResponseDto upsertAccounts(AccountsUpsertRequestDto request);
}
