package com.batu.account_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.entity.Account;
import com.batu.account_service.service.input.RecordAccountInput;
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

    AccountSummaryResponseDto getAccountSummary(Jwt principal);

    List<AccountNameResponseDto> getAccountsByGivenIds(AccountNameRequestDto request);

    Optional<Account> recordAccount(RecordAccountInput input);

    List<Account> deactivateByConnection(UUID connectionId, long version);
}
