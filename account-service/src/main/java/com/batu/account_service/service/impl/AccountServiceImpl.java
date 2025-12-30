package com.batu.account_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.account_service.CursorResponse;
import com.batu.account_service.entity.Account;
import com.batu.account_service.dto.AccountRequestDto;
import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.util.CursorUtils;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;

        public AccountServiceImpl(AccountRepository accountRepository, CursorUtils cursorUtils) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
        }

        @Override
        public CursorResponse<AccountResponseDto> getAccountsPaginated(
                        Jwt principal,
                        String accountName,
                        String institutionId,
                        String accountType,
                        String accountSubtype,
                        String cursor,
                        int limit,
                        AccountSortField sortBy,
                        Sort.Direction direction) {

                UUID userId = UUID.fromString(principal.getSubject());

                Sort sort = sortBy == null
                                ? Sort.by(direction, "createdAt").and(Sort.by("accountId"))
                                : Sort.by(direction, sortBy.getFieldName()).and(Sort.by("accountId"));

                ScrollPosition scrollPosition = cursor != null
                                ? cursorUtils.decode(cursor)
                                : ScrollPosition.keyset();

                var spec = AccountSpecification.filter(userId, accountName, institutionId, accountType, accountSubtype);

                Window<Account> accounts = accountRepository.findBy(spec, query -> query
                                .sortBy(sort)
                                .limit(limit)
                                .scroll(scrollPosition));

                List<AccountResponseDto> accountResponses = accounts.getContent().stream()
                                .map(acc -> new AccountResponseDto(
                                                acc.getAccountId(),
                                                acc.getConnectionId(),
                                                acc.getUserId(),
                                                acc.getExternalId(),
                                                acc.getAccountName(),
                                                acc.getAccountType(),
                                                acc.getAccountSubtype(),
                                                acc.getAccountMask(),
                                                acc.getCurrentBalance(),
                                                acc.getAvailableBalance(),
                                                acc.getIsoCurrentCode(),
                                                acc.isActive(),
                                                acc.getCreatedAt(),
                                                acc.getUpdatedAt()))
                                .toList();

                String nextCursor = accounts.hasNext()
                                ? cursorUtils.encode(accounts.positionAt(accounts.size() - 1))
                                : null;

                return new CursorResponse<>(accountResponses, accounts.hasNext(), nextCursor);
        }
}
