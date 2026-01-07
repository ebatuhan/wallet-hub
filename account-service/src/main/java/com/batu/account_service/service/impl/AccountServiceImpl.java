package com.batu.account_service.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.account_service.CursorResponse;

import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.dto.AccountViewDto;
import com.batu.account_service.entity.Account;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.exception.ResourceNotFoundException;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.util.CursorUtils;
import com.batu.shared.dto.AccountNameRequestDto;
import com.batu.shared.dto.AccountNameResponseDto;
import com.batu.shared.dto.AccountRequestDto;
import com.batu.shared.dto.AccountsUpsertRequestDto;
import com.batu.shared.dto.AccountsUpsertResponseDto;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;

        public AccountServiceImpl(AccountRepository accountRepository, CursorUtils cursorUtils) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
        }

        @Override
        public CursorResponse<AccountViewDto> getAccountsViewPaginated(
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

                Window<AccountViewDto> accounts = accountRepository.findBy(spec, query -> query
                                .as(AccountViewDto.class)
                                .sortBy(sort)
                                .limit(limit)
                                .scroll(scrollPosition));

                String nextCursor = accounts.hasNext()
                                ? cursorUtils.encode(accounts.positionAt(accounts.size() - 1))
                                : null;

                return new CursorResponse<>(accounts.getContent(), accounts.hasNext(), nextCursor);
        }

        @Override
        public AccountResponseDto getAccount(UUID accountId, Jwt principal) {

                UUID userId = UUID.fromString(principal.getSubject());

                return accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(accountId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "This account is not exists, or access restricted."));
        }

        @Override
        public List<AccountNameResponseDto> getAccountsByGivenIds(AccountNameRequestDto request) {
                return accountRepository.findByAccountIdIn(request.getAccountIds());
        }

        // Will be reworked to batch process
        @Override
        @Transactional
        public AccountsUpsertResponseDto upsertAccounts(AccountsUpsertRequestDto request) {

                Map<String, UUID> insertedAccountsMap = new HashMap<>();

                for (AccountRequestDto accRequest : request.getAccounts()) {

                        Account account = accountRepository
                                        .findByExternalId(accRequest.getExternalId())
                                        .orElseGet(() -> new Account(
                                                        accRequest.getConnectionId(),
                                                        accRequest.getUserId(),
                                                        accRequest.getExternalId(),
                                                        accRequest.getAccountName(),
                                                        accRequest.getAccountType(),
                                                        accRequest.getAccountSubtype(),
                                                        accRequest.getAccountMask(),
                                                        accRequest.getCurrentBalance(),
                                                        accRequest.getAvailableBalance(),
                                                        accRequest.getIsoCurrencyCode(),
                                                        accRequest.isActive()));

                        account.setAccountName(accRequest.getAccountName());
                        account.setAccountType(accRequest.getAccountType());
                        account.setAccountSubtype(accRequest.getAccountSubtype());
                        account.setCurrentBalance(accRequest.getCurrentBalance());
                        account.setAvailableBalance(accRequest.getAvailableBalance());
                        account.setIsoCurrencyCode(accRequest.getIsoCurrencyCode());

                        Account saved = accountRepository.save(account);

                        insertedAccountsMap.put(
                                        accRequest.getExternalId(),
                                        saved.getAccountId());
                }

                return new AccountsUpsertResponseDto(insertedAccountsMap);
        }

}
