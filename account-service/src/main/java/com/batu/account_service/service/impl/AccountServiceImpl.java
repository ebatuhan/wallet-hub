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
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.response.AccountCurrencyTotalDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

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
                return getAccount(accountId, UUID.fromString(principal.getSubject()));
        }

        @Override
        public AccountResponseDto getAccount(UUID accountId, UUID userId) {
                return accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(accountId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "This account is not exists, or access restricted."));
        }

        @Override
        public AccountSummaryResponseDto getAccountSummary(Jwt principal) {
                return getAccountSummary(UUID.fromString(principal.getSubject()));
        }

        @Override
        public AccountSummaryResponseDto getAccountSummary(UUID userId) {
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
        public void create(AccountRequestDto request) {
                Account account = accountSyncMapper.toEntity(request);
                accountRepository.save(account);
                publishPersistedEvents(List.of(account));
        }

        @Override
        @Transactional
        public void update(AccountRequestDto request) {
                Account account = accountRepository.findByAccountIdAndUserId(
                                request.getAccountId(),
                                request.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Account with id " + request.getAccountId() + " not found"));

                account.setInstitutionName(request.getInstitutionName());
                account.setAccountName(request.getAccountName());
                account.setAccountType(request.getAccountType());
                account.setAccountSubtype(request.getAccountSubtype());
                account.setAccountMask(request.getAccountMask());
                account.setCurrentBalance(request.getCurrentBalance());
                account.setAvailableBalance(request.getAvailableBalance());
                account.setIsoCurrencyCode(request.getIsoCurrencyCode());
                account.setActive(request.isActive());

                accountRepository.save(account);
                publishPersistedEvents(List.of(account));
        }

        @Override
        @Transactional
        public void deactivate(UUID accountId) {
                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Account with id " + accountId + " not found"));

                account.setActive(false);
                accountRepository.save(account);
                publishPersistedEvents(List.of(account));
        }

        private void publishPersistedEvents(List<Account> accounts) {
                eventPublisher.publishEvent(new AccountsPersistedDomainEvent(
                                accounts.stream()
                                                .map(accountSyncMapper::toPersistedEvent)
                                                .toList()));
        }

}
