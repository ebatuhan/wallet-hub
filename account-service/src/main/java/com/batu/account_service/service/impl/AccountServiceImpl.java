package com.batu.account_service.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.account_service.entity.Account;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.exception.ResourceNotFoundException;
import com.batu.account_service.messaging.AccountsPersistedDomainEvent;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.util.CursorUtils;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.AccountsUpsertRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.AccountsUpsertResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.messaging.event.AccountPersistedEvent;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;
        private final ApplicationEventPublisher eventPublisher;

        public AccountServiceImpl(AccountRepository accountRepository, CursorUtils cursorUtils,
                        ApplicationEventPublisher eventPublisher) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
                this.eventPublisher = eventPublisher;
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

        @Override
        @Transactional
        public AccountsUpsertResponseDto upsertAccounts(AccountsUpsertRequestDto request) {
                Map<String, UUID> insertedAccountsMap = new HashMap<>();
                List<AccountPersistedEvent> persistedAccounts = new ArrayList<>();

                for (AccountRequestDto accRequest : request.getAccounts()) {
                        Account savedAccount = accountRepository.upsertAccounts(
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
                                        accRequest.isActive());

                        insertedAccountsMap.put(savedAccount.getExternalId(), savedAccount.getAccountId());
                        persistedAccounts.add(new AccountPersistedEvent(
                                        UUID.randomUUID(),
                                        Instant.now(),
                                        "account-service",
                                        savedAccount.getAccountId(),
                                        savedAccount.getConnectionId(),
                                        savedAccount.getUserId(),
                                        savedAccount.getExternalId(),
                                        savedAccount.getAccountName(),
                                        savedAccount.getAccountType(),
                                        savedAccount.getAccountSubtype(),
                                        savedAccount.getAccountMask(),
                                        savedAccount.getCurrentBalance(),
                                        savedAccount.getAvailableBalance(),
                                        savedAccount.getIsoCurrencyCode(),
                                        savedAccount.isActive()));
                }

                if (!persistedAccounts.isEmpty()) {
                        eventPublisher.publishEvent(new AccountsPersistedDomainEvent(persistedAccounts));
                }

                return new AccountsUpsertResponseDto(insertedAccountsMap);
        }

}
