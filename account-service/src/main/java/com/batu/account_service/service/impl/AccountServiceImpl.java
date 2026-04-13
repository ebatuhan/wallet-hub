package com.batu.account_service.service.impl;

import java.util.List;
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
import com.batu.account_service.mapper.AccountSyncMapper;
import com.batu.account_service.messaging.AccountsPersistedDomainEvent;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.util.CursorUtils;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountCurrencyTotalDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.messaging.command.AccountSyncCommand;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;
        private final ApplicationEventPublisher eventPublisher;
        private final AccountSyncMapper accountSyncMapper;

        public AccountServiceImpl(AccountRepository accountRepository,
                        CursorUtils cursorUtils,
                        ApplicationEventPublisher eventPublisher,
                        AccountSyncMapper accountSyncMapper) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
                this.eventPublisher = eventPublisher;
                this.accountSyncMapper = accountSyncMapper;
        }

        @Override
        public CursorResponse<AccountViewDto> getAccountsViewPaginated(
                        Jwt principal,
                        String accountName,
                        String institutionName,
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

                var spec = AccountSpecification.filter(userId, accountName, institutionName, accountType, accountSubtype);

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
        public AccountSummaryResponseDto getAccountSummary(Jwt principal) {
                UUID userId = UUID.fromString(principal.getSubject());

                List<AccountCurrencyTotalDto> totalsByCurrency = accountRepository.summarizeActiveBalancesByCurrency(userId)
                                .stream()
                                .map(total -> new AccountCurrencyTotalDto(
                                                total.getIsoCurrencyCode(),
                                                total.getCurrentBalanceTotal(),
                                                total.getAvailableBalanceTotal()))
                                .toList();

                return new AccountSummaryResponseDto(
                                userId,
                                accountRepository.countByUserIdAndIsActiveTrue(userId),
                                totalsByCurrency);
        }

        @Override
        public List<AccountNameResponseDto> getAccountsByGivenIds(AccountNameRequestDto request) {
                return accountRepository.findByAccountIdIn(request.getAccountIds());
        }

        @Override
        @Transactional
        public void create(AccountSyncCommand command) {
                if (accountRepository.findByAccountIdAndUserId(command.getAccountId(), command.getUserId()).isPresent()) {
                        return;
                }

                try {
                        accountRepository.insertSyncedAccount(
                                        command.getAccountId(),
                                        command.getUserId(),
                                        command.getInstitutionName(),
                                        command.getAccountName(),
                                        command.getAccountType(),
                                        command.getAccountSubtype(),
                                        command.getAccountMask(),
                                        command.getCurrentBalance(),
                                        command.getAvailableBalance(),
                                        command.getIsoCurrencyCode(),
                                        command.isActive());
                } catch (RuntimeException ex) {
                        if (accountRepository.findByAccountIdAndUserId(command.getAccountId(), command.getUserId()).isPresent()) {
                                return;
                        }

                        throw ex; //TODO very bad will fix later.
                }

                eventPublisher.publishEvent(new AccountsPersistedDomainEvent(
                                java.util.List.of(accountSyncMapper.toPersistedEvent(command))));
        }

        @Override
        @Transactional
        public void update(AccountSyncCommand command) {
                if (!command.isActive()) {
                        deactivate(command);
                        return;
                }

                int updatedRows = accountRepository.updateSyncedAccount(
                                command.getAccountId(),
                                command.getUserId(),
                                command.getInstitutionName(),
                                command.getAccountName(),
                                command.getAccountType(),
                                command.getAccountSubtype(),
                                command.getAccountMask(),
                                command.getCurrentBalance(),
                                command.getAvailableBalance(),
                                command.getIsoCurrencyCode(),
                                command.isActive());

                if (updatedRows == 0) {
                        create(command);
                        return;
                }

                eventPublisher.publishEvent(new AccountsPersistedDomainEvent(
                                java.util.List.of(accountSyncMapper.toPersistedEvent(command))));
        }

        private void deactivate(AccountSyncCommand command) {
                Account account = accountRepository.findByAccountIdAndUserId(
                                command.getAccountId(),
                                command.getUserId())
                                .orElse(null);

                if (account == null) {
                        return;
                }

                if (!account.isActive()) {
                        return;
                }

                int updatedRows = accountRepository.deactivateSyncedAccount(
                                command.getAccountId(),
                                command.getUserId());

                if (updatedRows == 0) {
                        return;
                }

                account.setActive(false);

                eventPublisher.publishEvent(new AccountsPersistedDomainEvent(
                                java.util.List.of(accountSyncMapper.toPersistedEvent(account))));
        }
}
