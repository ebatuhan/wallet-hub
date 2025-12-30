package com.batu.account_service.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.account_service.CursorResponse;
import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.enums.AccountSortField;

public interface AccountService {

    CursorResponse<AccountResponseDto> getAccountsPaginated(Jwt principal,
            String accountName,
            String institutionId,
            String accountType,
            String accountSubtype,
            String cursor,
            int limit,
            AccountSortField sortBy,
            Sort.Direction direction);
}
