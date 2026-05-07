package com.batu.account_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import io.micrometer.observation.annotation.Observed;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.batu.account_service.entity.Account;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.messaging.OutboxDomainEventPublisher;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.dto.response.AccountCurrencyTotalDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountUpsertResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;
        private final OutboxDomainEventPublisher eventPublisher;

        public AccountServiceImpl(AccountRepository accountRepository,
                        CursorUtils cursorUtils,
                        OutboxDomainEventPublisher eventPublisher) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
                this.eventPublisher = eventPublisher;
        }

        @Override
        @Observed(name = "account.list", contextualName = "account list accounts")
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

                Window<Account> accounts = accountRepository.findBy(spec, query -> query
                                .sortBy(sort)
                                .limit(limit)
                                .scroll(scrollPosition));

                String nextCursor = accounts.hasNext()
                                ? cursorUtils.encode(accounts.positionAt(accounts.size() - 1))
                                : null;

                List<AccountViewDto> content = accounts.getContent().stream()
                                .map(account -> new AccountViewDto(
                                                account.getAccountId(),
                                                account.getInstitutionName(),
                                                account.getAccountName(),
                                                account.getCurrentBalance(),
                                                account.getAvailableBalance(),
                                                account.getIsoCurrencyCode(),
                                                account.getAccountType(),
                                                account.getAccountSubtype(),
                                                account.getAccountMask(),
                                                account.getCreatedAt(),
                                                account.getUpdatedAt()))
                                .toList();

                return new CursorResponse<>(content, accounts.hasNext(), nextCursor);
        }

        @Override
        public AccountResponseDto getAccount(UUID accountId, Jwt principal) {
                UUID userId = UUID.fromString(principal.getSubject());
                return accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(accountId, userId)
                                .map(this::toAccountResponse)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
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
        public List<AccountResponseDto> findAccountsByConnectionId(UUID connectionId) {
                return accountRepository.findByConnectionIdAndIsActiveTrueOrderByCreatedAtDesc(connectionId).stream()
                                .map(this::toAccountResponse)
                                .toList();
        }

        @Override
        @Transactional
        public AccountUpsertResponseDto upsertAccount(AccountUpsertRequestDto request) {
                Account account = accountRepository.upsertAccount(request);
                eventPublisher.publishAccountRecorded(new AccountRecorded(
                                account.getAccountId(),
                                account.getUserId(),
                                account.getConnectionId(),
                                account.getInstitutionName(),
                                account.getAccountName(),
                                account.getAccountType(),
                                account.getAccountSubtype(),
                                account.getAccountMask(),
                                account.getCurrentBalance(),
                                account.getAvailableBalance(),
                                account.getIsoCurrencyCode(),
                                account.isActive()));
                return toUpsertResponse(account);
        }

        @Override
        @Transactional
        public List<AccountUpsertResponseDto> deactivateAccountsByConnection(UUID connectionId) {
                List<Account> accounts = accountRepository.findByConnectionIdAndIsActiveTrue(connectionId);

                for (Account account : accounts) {
                        account.setActive(false);
                }

                List<Account> savedAccounts = accountRepository.saveAll(accounts);

                for (Account account : savedAccounts) {
                        eventPublisher.publishAccountRemoved(new AccountRemoved(
                                        account.getAccountId(),
                                        account.getUserId(),
                                        account.getConnectionId()));
                }

                return savedAccounts.stream().map(this::toUpsertResponse).toList();
        }

        private AccountUpsertResponseDto toUpsertResponse(Account account) {
                return new AccountUpsertResponseDto(
                                account.getAccountId(),
                                account.getUserId(),
                                account.getConnectionId(),
                                account.getInstitutionName(),
                                account.getAccountName(),
                                account.getAccountType(),
                                account.getAccountSubtype(),
                                account.getAccountMask(),
                                account.getCurrentBalance(),
                                account.getAvailableBalance(),
                                account.getIsoCurrencyCode(),
                                account.isActive(),
                                account.getCreatedAt(),
                                account.getUpdatedAt());
        }

        private AccountResponseDto toAccountResponse(Account account) {
                return new AccountResponseDto(
                                account.getAccountId(),
                                account.getInstitutionName(),
                                account.getAccountName(),
                                account.getAccountType(),
                                account.getAccountSubtype(),
                                account.getAccountMask(),
                                account.getCurrentBalance(),
                                account.getAvailableBalance(),
                                account.getIsoCurrencyCode(),
                                account.getCreatedAt(),
                                account.getUpdatedAt());
        }
}
